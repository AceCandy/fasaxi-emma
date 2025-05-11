package cn.acecandy.fasaxi.emma.config;

import cn.acecandy.fasaxi.emma.common.enums.EmbyPicType;
import cn.acecandy.fasaxi.emma.service.OriginReqService;
import cn.acecandy.fasaxi.emma.service.PicRedirectService;
import cn.acecandy.fasaxi.emma.service.VideoRedirectService;
import cn.acecandy.fasaxi.emma.utils.FileCacheUtil;
import cn.acecandy.fasaxi.emma.utils.ReUtil;
import cn.acecandy.fasaxi.emma.utils.ThreadUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.date.DateUtil;
import org.dromara.hutool.core.math.NumberUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.core.text.split.SplitUtil;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_416;
import static cn.acecandy.fasaxi.emma.common.enums.EmbyPicType.非图片;
import static cn.acecandy.fasaxi.emma.utils.EmbyProxyUtil.getContentType;
import static cn.acecandy.fasaxi.emma.utils.EmbyProxyUtil.getPicType;
import static cn.acecandy.fasaxi.emma.utils.EmbyProxyUtil.needClose;

/**
 * 转发所有emby请求
 *
 * @author tangningzhu
 * @since 2025/4/15
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@WebFilter("/emby/*")
public class EmbyProxyFilter implements Filter {

    @Resource
    private EmbyConfig embyConfig;

    @Resource
    private FileCacheUtil fileCacheUtil;

    @Resource
    private AccessLog accessLog;

    @Resource
    private PicRedirectService picRedirectService;

    @Resource
    private VideoRedirectService videoRedirectService;

    @Resource
    private OriginReqService originReqService;


    @Override
    @SneakyThrows
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        long start = DateUtil.current();
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        /*if (isWebSocketHandshake(req)) {
            log.warn("WebSocket请求: {}", req.getRequestURI());
            handleWebSocket(req, res);
            return;
        }*/

        EmbyContentCacheReqWrapper reqWrapper = new EmbyContentCacheReqWrapper(req);
        try {
            if (needClose(req)) {
                res.setStatus(CODE_416);
                return;
            }
            if (!StrUtil.containsAny(reqWrapper.getUa(), "okhttp",
                    "Yamby", "Hills", "AfuseKt",
                    "SenPlayer", "VidHub", "Forward")) {
                log.error("非法UA已被禁止: {}", reqWrapper.getUa());
                res.setStatus(CODE_416);
                return;
            }
            EmbyPicType picType = getPicType(req);
            if (!非图片.equals(picType)) {
                picRedirectService.processPic(reqWrapper, res, picType);
            } else {
                String mediaSourceId = ReUtil.isVideoUrl(req.getRequestURI());
                if (StrUtil.isNotBlank(mediaSourceId)) {
                    videoRedirectService.processVideo(reqWrapper, res);
                } else {
                    originReqService.forwardOriReq(reqWrapper, res);
                }
            }
        } catch (Exception e) {
            log.warn("转发请求失败[{}]: {}?{}", req.getMethod(), reqWrapper.getRequestURI(), reqWrapper.getQueryString(), e);
            originReqService.forwardOriReq(reqWrapper, res);
        } finally {
            accessLog.log(reqWrapper.getMethod(), reqWrapper.getRequestURI(), reqWrapper.getIp(),
                    req.getQueryString(), res.getStatus(), start);
        }
    }

    private boolean isWebSocketHandshake(HttpServletRequest request) {
        String connection = request.getHeader("Connection");
        String upgrade = request.getHeader("Upgrade");
        return "Upgrade".equalsIgnoreCase(connection) &&
                "websocket".equalsIgnoreCase(upgrade);
    }

    // WebSocket请求转发实现
    @SneakyThrows
    private void handleWebSocket(HttpServletRequest req, HttpServletResponse res) {
        try {
            URI targetUri = new URI(embyConfig.getWsHost() + req.getRequestURI());
            HttpClient client = HttpClient.newHttpClient();
            AsyncContext asyncContext = req.startAsync();

            WebSocket targetSocket = client.newWebSocketBuilder()
                    // .header("Sec-WebSocket-Key", req.getHeader("Sec-WebSocket-Key"))
                    // .header("Sec-WebSocket-Version", req.getHeader("Sec-WebSocket-Version"))
                    // .header("Upgrade", "websocket")
                    // .header("Connection", "Upgrade")
                    .buildAsync(targetUri, new WebSocket.Listener() {
                        private StringBuilder textBuffer = new StringBuilder();

                        @Override
                        public void onOpen(WebSocket webSocket) {
                            webSocket.request(1);
                            log.info("WebSocket连接已建立: {}", targetUri);
                        }

                        @SneakyThrows
                        @Override
                        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                            textBuffer.append(data);
                            if (last) {
                                log.info("转发服务端消息: {}", textBuffer);
                                asyncContext.getResponse().getWriter().write(textBuffer.toString());
                                textBuffer.setLength(0);
                            }
                            webSocket.request(1);
                            return null;
                        }

                        @Override
                        public void onError(WebSocket webSocket, Throwable error) {
                            log.error("WebSocket错误: {}", error.getMessage());
                            webSocket.abort();
                            asyncContext.complete();
                        }
                    }).get(30, TimeUnit.SECONDS);

            ServletInputStream input = req.getInputStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            input.setReadListener(new ReadListener() {
                @Override
                public void onDataAvailable() throws IOException {
                    byte[] chunk = new byte[input.available()];
                    int read = input.read(chunk);
                    if (read > 0) {
                        buffer.write(chunk, 0, read);
                    }
                }

                @Override
                public void onAllDataRead() {
                    targetSocket.sendText(buffer.toString(StandardCharsets.UTF_8), true);
                    buffer.reset();
                }

                @Override
                public void onError(Throwable t) {
                    log.error("输入流异常: {}", t.getMessage());
                    targetSocket.abort();
                    asyncContext.complete();
                }
            });

        } catch (TimeoutException e) {
            log.error("连接超时: {}", e.getMessage());
            res.sendError(504, "WebSocket握手超时");
        } catch (Exception e) {
            log.error("内部错误: {}", e.getMessage());
            res.sendError(500, "服务器内部错误");
        }
    }


    @SneakyThrows
    private boolean findRangeCache(EmbyContentCacheReqWrapper request, HttpServletResponse response,
                                   FileCacheUtil.FileInfo embyInfo) {
        log.info("查找本地缓存文件->");
        embyInfo.setCacheStatus(FileCacheUtil.CacheStatus.PARTIAL);
        String range = request.getHeader("Range");
        if (!StrUtil.startWithIgnoreCase(range, "bytes=")) {
            embyInfo.setStartByte(0L);
            embyInfo.setCacheStatus(FileCacheUtil.CacheStatus.PARTIAL);
            if (fileCacheUtil.checkCacheFile(embyInfo)) {
                response.setHeader("Cache-Control", "private, no-transform, no-cache");
                response.setHeader("X-EmbyToAList-Cache", "Hit");
                response.setHeader("Content-Length", String.valueOf(embyInfo.getSize()));
                // response.setHeader("Content-Length", String.valueOf(embyInfo.getCacheFileSize()));
                // response.setHeader("X-Chunk-Size", String.valueOf(embyInfo.getCacheFileSize()));
                response.setHeader("Content-Range", StrUtil.format("bytes {}-{}/{}",
                        embyInfo.getStartByte(), embyInfo.getCacheFileSize() - 1, embyInfo.getSize()));
                // Content-Range: bytes 0-9999/100000  // 当前分片是 0-9999 字节（大小 10000 字节）

                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                Stream<byte[]> cache = fileCacheUtil.readCacheFile(embyInfo);
                cache.forEach(c -> {
                    try {
                        response.getOutputStream().write(c, 0, c.length);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

                return true;
            } else {
                ThreadUtil.execVirtual(() -> fileCacheUtil.writeCacheFile(embyInfo, request.getCachedHeader()));
                response.setStatus(HttpServletResponse.SC_FOUND);
                response.setHeader("Location", embyInfo.getRealUrl());
                return false;
            }
        } else {
            String byteRange = CollUtil.getLast(SplitUtil.split(range, "="));
            List<String> rangeList = SplitUtil.split(byteRange, "-");
            Long startByte = NumberUtil.parseLong(CollUtil.getFirst(rangeList), null);
            Long endByte = NumberUtil.parseLong(CollUtil.getLast(rangeList), null);
            embyInfo.setStartByte(startByte);
            embyInfo.setEndByte(endByte);
            embyInfo.setCacheStatus(FileCacheUtil.CacheStatus.PARTIAL);
            if (fileCacheUtil.checkCacheFile(embyInfo)) {
                response.setHeader("Cache-Control", "private, no-transform, no-cache");
                response.setHeader("Content-Length", String.valueOf(embyInfo.getSize()));
                response.setHeader("X-EmbyToAList-Cache", "Hit");
                // response.setHeader("X-Chunk-Size", String.valueOf(embyInfo.getCacheFileSize()));
                Long endPoint = Math.min(embyInfo.getSize(),
                        embyInfo.getStartByte() + embyInfo.getCacheFileSize()) - 1;
                response.setHeader("Content-Range", StrUtil.format("bytes {}-{}/{}",
                        embyInfo.getStartByte(), endPoint, embyInfo.getSize()));
                // Content-Range: bytes 0-9999/100000  // 当前分片是 0-9999 字节（大小 10000 字节）

                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                Stream<byte[]> cache = fileCacheUtil.readCacheFile(embyInfo);
                cache.forEach(c -> {
                    try {
                        response.getOutputStream().write(c, 0, c.length);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

                return true;
            } else {
                ThreadUtil.execVirtual(() -> fileCacheUtil.writeCacheFile(embyInfo, request.getCachedHeader()));
                response.setStatus(HttpServletResponse.SC_FOUND);
                response.setHeader("Location", embyInfo.getRealUrl());
                return false;
            }
        }
        // return false;
    }

    @SneakyThrows
    private boolean rangeCache(EmbyContentCacheReqWrapper request, HttpServletResponse response,
                               FileCacheUtil.FileInfo embyInfo) {
        String range = request.getHeader("Range");
        log.info("读取缓存中:{}", range);
        if (!StrUtil.startWithIgnoreCase(range, "bytes=")) {
            log.warn("范围标头非法: {}", request.getCachedHeader());
            embyInfo.setStartByte(0L);
            embyInfo.setCacheStatus(FileCacheUtil.CacheStatus.PARTIAL);
            if (fileCacheUtil.checkCacheFile(embyInfo)) {
                response.setHeader("Cache-Control", "private, no-transform, no-cache");
                response.setHeader("Content-Length", String.valueOf(embyInfo.getSize()));
                response.setHeader("X-EmbyToAList-Cache", "Hit");

                Map<String, String> headers = request.getCachedHeader();
                headers.put("Range", StrUtil.format("bytes={}-", embyInfo.getCacheFileSize()));
                mixFileCache(embyInfo, response, headers);
                return true;
            } else {
                ThreadUtil.execVirtual(() -> fileCacheUtil.writeCacheFile(embyInfo, request.getCachedHeader()));
                response.setStatus(HttpServletResponse.SC_FOUND);
                response.setHeader("Location", embyInfo.getRealUrl());
                return true;
            }
        } else {
            String byteRange = CollUtil.getLast(SplitUtil.split(range, "="));
            List<String> rangeList = SplitUtil.split(byteRange, "-");
            Long startByte = NumberUtil.parseLong(CollUtil.getFirst(rangeList), null);
            Long endByte = NumberUtil.parseLong(CollUtil.getLast(rangeList), null);
            embyInfo.setStartByte(startByte);
            embyInfo.setEndByte(endByte);
            if (startByte >= embyInfo.getSize()) {
                log.warn("range请求超出文件大小[{}]: {}", embyInfo.getSize(), request.getCachedHeader());
                response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return true;
            }
            Long cacheFileSize = embyInfo.getCacheFileSize();
            Long respEndByte;
            Long respFileSize;
            String sourceRange;
            // 应该走缓存的情况1：请求文件开头
            if (startByte < cacheFileSize) {
                if (endByte == null || endByte > cacheFileSize) {
                    embyInfo.setCacheStatus(FileCacheUtil.CacheStatus.PARTIAL);
                } else {
                    embyInfo.setCacheStatus(FileCacheUtil.CacheStatus.HIT);
                }
                // 如果请求末尾在cache范围内
                // 如果请求末尾在缓存文件大小之外，取缓存文件大小；否则取请求末尾
                Long cacheEndByte = FileCacheUtil.CacheStatus.PARTIAL.equals(embyInfo.getCacheStatus())
                        ? cacheFileSize : endByte;
                respEndByte = (endByte == null || endByte > cacheEndByte)
                        ? embyInfo.getSize() - 1 : cacheEndByte;
                if (fileCacheUtil.checkCacheFile(embyInfo)) {
                    response.setHeader("Content-Type", getContentType(embyInfo.getContainer()));
                    response.setHeader("Accept-Ranges", "bytes");
                    response.setHeader("Content-Range", StrUtil.format("bytes {}-{}/{}",
                            startByte, respEndByte, embyInfo.getSize()));
                    response.setHeader("Content-Length", String.valueOf(respEndByte - startByte + 1));
                    response.setHeader("Cache-Control", "private, no-transform, no-cache");
                    response.setHeader("X-EmbyToAList-Cache", "Hit");


                    if (FileCacheUtil.CacheStatus.MISS.equals(embyInfo.getCacheStatus())) {
                        // 完全超出范围
                        if (endByte != null) {
                            sourceRange = StrUtil.format("bytes={}-{}", startByte, endByte - 1);
                        } else {
                            sourceRange = StrUtil.format("bytes={}", startByte);
                        }
                        Map<String, String> headers = request.getCachedHeader();
                        headers.put("Range", sourceRange);
                        mixFileCache(embyInfo, response, headers);
                        return true;
                    } else if (StrUtil.equalsAny(embyInfo.getCacheStatus().name(),
                            FileCacheUtil.CacheStatus.HIT.name(), FileCacheUtil.CacheStatus.HIT_TAIL.name())) {
                        // 完全在范围内
                        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                        mixFileCache(embyInfo, response, request.getCachedHeader());
                        return true;
                    } else {
                        // 请求的范围与缓存重叠并超出缓存
                        Long sourceStart = cacheFileSize;
                        if (endByte != null) {
                            sourceRange = StrUtil.format("bytes={}-{}", sourceStart, endByte - 1);
                        } else {
                            sourceRange = StrUtil.format("bytes={}", sourceStart);
                        }
                        Map<String, String> headers = request.getCachedHeader();
                        headers.put("Range", sourceRange);
                        mixFileCache(embyInfo, response, headers);
                        return true;
                    }
                } else {
                    ThreadUtil.execVirtual(() -> fileCacheUtil.writeCacheFile(embyInfo, request.getCachedHeader()));
                    response.setStatus(HttpServletResponse.SC_FOUND);
                    response.setHeader("Location", embyInfo.getRealUrl());
                }
            } else if (embyInfo.getSize() - startByte < 2 * 1024 * 1024) {
                // 应该走缓存的情况2：请求文件末尾
                embyInfo.setCacheStatus(FileCacheUtil.CacheStatus.HIT_TAIL);
                if (fileCacheUtil.checkCacheFile(embyInfo)) {
                    if (endByte == null) {
                        respEndByte = embyInfo.getSize() - 1;
                        respFileSize = respEndByte + 1 - startByte;
                    } else {
                        respEndByte = endByte;
                        respFileSize = endByte + 1 - startByte;
                    }
                    response.setHeader("Content-Type", getContentType(embyInfo.getContainer()));
                    response.setHeader("Accept-Ranges", "bytes");
                    response.setHeader("Content-Range", StrUtil.format("bytes {}-{}/{}",
                            startByte, respEndByte, embyInfo.getSize()));
                    response.setHeader("Content-Length", String.valueOf(respFileSize));
                    response.setHeader("Cache-Control", "private, no-transform, no-cache");
                    response.setHeader("X-EmbyToAList-Cache", "Hit");

                    response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                    mixFileCache(embyInfo, response, request.getCachedHeader());
                } else {
                    ThreadUtil.execVirtual(() -> fileCacheUtil.writeCacheFile(embyInfo, request.getCachedHeader()));
                    response.setStatus(HttpServletResponse.SC_FOUND);
                    response.setHeader("Location", embyInfo.getRealUrl());
                }
            } else {
                // 请求范围在缓存范围之外
                embyInfo.setCacheStatus(FileCacheUtil.CacheStatus.MISS);
                response.setHeader("Content-Type", getContentType(embyInfo.getContainer()));
                response.setHeader("Accept-Ranges", "bytes");
                response.setHeader("Content-Range", StrUtil.format("bytes {}-{}/{}",
                        startByte, embyInfo.getSize() - 1, embyInfo.getSize()));
                response.setHeader("Content-Length", String.valueOf(embyInfo.getSize() - startByte));
                response.setHeader("Cache-Control", "private, no-transform, no-cache");
                response.setHeader("X-EmbyToAList-Cache", "Miss");

                if (FileCacheUtil.CacheStatus.MISS.equals(embyInfo.getCacheStatus())) {
                    // 完全超出范围
                    if (endByte != null) {
                        sourceRange = StrUtil.format("bytes={}-{}", startByte, endByte - 1);
                    } else {
                        sourceRange = StrUtil.format("bytes={}", startByte);
                    }
                    Map<String, String> headers = request.getCachedHeader();
                    headers.put("Range", sourceRange);
                    mixFileCache(embyInfo, response, headers);
                    return true;
                }
            }
        }
        return false;
    }

    @SneakyThrows
    private void mixFileCache(FileCacheUtil.FileInfo embyInfo, HttpServletResponse response,
                              Map<String, String> headers) {
        Stream<byte[]> cache = fileCacheUtil.readCacheFile(embyInfo);
        // InputStream cache = fileCacheUtil.readCacheFileInput(embyInfo);
        response.setContentType(getContentType(embyInfo.getContainer()));
        // response.setContentLength(embyInfo.getCacheFileSize().intValue());
        // response.setHeader("Transfer-Encoding", "chunked");

        // response.setHeader("Content-Range", "bytes " + 0 + "-" + embyInfo.getCacheFileSize() + "/" + embyInfo.getSize());
        // response.setHeader("Accept-Ranges", "bytes");
        // response.setHeader("Range", StrUtil.format("bytes={}-", embyInfo.getCacheFileSize()));
        try (ServletOutputStream out = response.getOutputStream()) {
            cache.forEach(b -> {
                try {
                    out.write(b, 0, 1024 * 1024);
                    out.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        /*try {
            ServletOutputStream outputStream = response.getOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = cache.read(buffer)) != -1) {
                if (!response.isCommitted()) { // 新增连接状态检查
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                } else {
                    log.debug("连接已断开，终止写入");
                    break;
                }
            }
        } catch (Exception e) {
            log.debug("客户端主动断开: {}", e.getMessage());
        } finally {
            IoUtil.close(cache);
        }*/
        /*ServletOutputStream out = null;
        try {
            out = response.getOutputStream();
            IoUtil.copy(cache, out, IoUtil.DEFAULT_BUFFER_SIZE);
        } catch (IOException e) {
            throw new UtilException(e);
        } finally {
            // IoUtil.close(out);
            IoUtil.close(cache);
        }*/
        // OutputStream outputStream = response.getOutputStream();
        /*byte[] mergedBytes = cache.toList().stream()
                .reduce(new ByteArrayOutputStream(), (bos, bytes) -> {
                            bos.write(bytes, 0, bytes.length);
                            return bos;
                        },
                        (bos1, bos2) -> bos1)
                .toByteArray();
        outputStream.write(mergedBytes);
        outputStream.flush();*/
/*
        OutputStream outputStream = null;
        try {
            outputStream = response.getOutputStream();
            cache.forEach(bytes -> {
                try {
                    outputStream.write(bytes);
                } catch (IOException e) {
                    log.error("写入缓存文件失败: {}", embyInfo.getPath(), e);
                }
            });
            outputStream.flush();
        } catch (IOException e) {
            log.error("获取输出流或写入数据时出错", e);
        } finally {
            IoUtil.close(outputStream);
        }*/
        log.info("缓存已用尽");


        // request.getHeaderMap().put("host", URI.create(urlTask).getHost());
        /*try (HttpResponse res = HttpUtil.createGet(embyInfo.getRealUrl())
                .headerMap(headers, true).execute()) {
            outputStream.write(res.bodyBytes());
        }*/
    }

}