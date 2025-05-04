package cn.acecandy.fasaxi.emma.config;

import cn.acecandy.fasaxi.emma.common.enums.EmbyPicType;
import cn.acecandy.fasaxi.emma.common.ex.BaseException;
import cn.acecandy.fasaxi.emma.common.resp.EmbyCachedResp;
import cn.acecandy.fasaxi.emma.dao.entity.EmbyItemPic;
import cn.acecandy.fasaxi.emma.dao.service.EmbyItemPicDao;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.out.EmbyRemoteImageOut;
import cn.acecandy.fasaxi.emma.sao.proxy.EmbyProxy;
import cn.acecandy.fasaxi.emma.utils.EmbyUtil;
import cn.acecandy.fasaxi.emma.utils.FileCacheUtil;
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
import org.dromara.hutool.core.array.ArrayUtil;
import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.date.DateUtil;
import org.dromara.hutool.core.date.StopWatch;
import org.dromara.hutool.core.exception.ExceptionUtil;
import org.dromara.hutool.core.map.MapUtil;
import org.dromara.hutool.core.math.NumberUtil;
import org.dromara.hutool.core.net.url.UrlQueryUtil;
import org.dromara.hutool.core.regex.ReUtil;
import org.dromara.hutool.core.regex.RegexPool;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.core.text.split.SplitUtil;
import org.dromara.hutool.core.thread.ThreadUtil;
import org.dromara.hutool.http.HttpUtil;
import org.dromara.hutool.http.client.Request;
import org.dromara.hutool.http.client.Response;
import org.dromara.hutool.http.client.engine.ClientEngine;
import org.dromara.hutool.http.meta.HttpStatus;
import org.dromara.hutool.http.meta.Method;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import static cn.acecandy.fasaxi.emma.common.enums.EmbyPicType.Logo;
import static cn.acecandy.fasaxi.emma.common.enums.EmbyPicType.封面;
import static cn.acecandy.fasaxi.emma.common.enums.EmbyPicType.背景图;
import static cn.acecandy.fasaxi.emma.common.enums.EmbyPicType.非图片;
import static io.undertow.util.StatusCodes.OK;

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
    private ClientEngine httpClient;

    @Resource
    private EmbyConfig embyConfig;

    @Resource
    private EmbyUtil embyUtil;

    @Resource
    private EmbyProxy embyProxy;

    @Resource
    private FileCacheUtil fileCacheUtil;

    @Resource
    private EmbyItemPicDao embyItemPicDao;

    @Resource
    private TmdbConfig tmdbConfig;

    @Resource
    private RedisClient redisClient;

    private final Map<String, Lock> urlLockMap = MapUtil.newSafeConcurrentHashMap();
    private final Map<String, Lock> picLockMap = MapUtil.newSafeConcurrentHashMap();


    @Override
    @SneakyThrows
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
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
                res.setStatus(HttpStatus.HTTP_REQUESTED_RANGE_NOT_SATISFIABLE);
                log.info("请求屏蔽: {}", req.getRequestURI());
                return;
            }
            EmbyPicType picType = needPicRedirect(req);
            if (!非图片.equals(picType)) {
                processPic(reqWrapper, res, picType);
            } else if (needVideoRedirect(req)) {
                processVideo(reqWrapper, res);
            } else {
                forwardOriReq(reqWrapper, res);
            }
        } catch (Exception e) {
            log.warn("转发请求失败[{}]: {}", req.getMethod(), reqWrapper.getRequestURI(), e);
            forwardOriReq(reqWrapper, res);
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

    private EmbyPicType needPicRedirect(HttpServletRequest req) {
        EmbyPicType type = 非图片;
        String url = req.getRequestURI().toLowerCase();
        if (!StrUtil.contains(url, "/items/")) {
            return type;
        }
        if (StrUtil.contains(url, "/images/primary")) {
            type = 封面;
        } else if (StrUtil.contains(url, "/images/backdrop")) {
            type = 背景图;
        } else if (StrUtil.contains(url, "/images/logo")) {
            type = Logo;
        }
        return type;
    }

    private boolean needVideoRedirect(HttpServletRequest req) {
        return StrUtil.containsAll(req.getRequestURI().toLowerCase(), "/emby/videos/", "/original");
    }

    private boolean needClose(HttpServletRequest req) {
        return StrUtil.containsAnyIgnoreCase(req.getRequestURI(), "/embywebsocket");
    }

    /**
     * 静态资源
     *
     * @param req 请求入参
     * @return boolean
     */
    private boolean isCacheReq(HttpServletRequest req) {
        String uri = req.getRequestURI().toLowerCase();
        if (StrUtil.containsAnyIgnoreCase(uri, "/images/primary", "/images/backdrop", "/images/logo")) {
            return true;
        }
        return uri.matches(".*\\.(js|css|woff2|png|jpg|gif|ico|json|html)$");
    }

    /**
     * 静态资源缓存key
     *
     * @param req 请求入参
     * @return {@link String }
     */
    private String staticCacheKey(EmbyContentCacheReqWrapper req) {
        return StrUtil.format("{}?{}", req.getRequestURI(), req.getCachedParam());
    }

    /**
     * 处理特殊请求
     *
     * @param request  要求
     * @param response 响应
     */
    @SneakyThrows
    private void processVideo(EmbyContentCacheReqWrapper request, HttpServletResponse response) {
        Map<String, Object> params = request.getCachedParam();
        String mediaSourceId = StrUtil.removePrefixIgnoreCase(
                MapUtil.getStr(params, "MediaSourceId"), "mediasource_");
        if (StrUtil.isBlank(mediaSourceId)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String ua = request.getUa();
        String cacheKey = ua + "|" + mediaSourceId;
        String cacheUrl = redisClient.getStrFindOne(ua + "|" + mediaSourceId, mediaSourceId);
        if (StrUtil.isNotBlank(cacheUrl)) {
            cacheUrl = getPtUrl(cacheUrl);
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", cacheUrl);
            log.warn("重定向(缓存):[{}] => {}", cacheKey, cacheUrl);
            return;
        }
        // 获取或创建对应的锁
        Lock lock = urlLockMap.computeIfAbsent(mediaSourceId, k -> new ReentrantLock());
        if (!lock.tryLock(15, TimeUnit.SECONDS)) {
            log.warn("请求锁被占用: {}", mediaSourceId);
            response.setStatus(429);
            return;
        }
        try {
            exec302(request, response, mediaSourceId);
        } finally {
            lock.unlock();
            urlLockMap.remove(mediaSourceId);
        }
    }

    private String getPtUrl(String cacheUrl) {
        if (!StrUtil.startWithIgnoreCase(cacheUrl, embyConfig.getOriginPt())) {
            return cacheUrl;
        }
        int minute = DateUtil.thisMinute();
        if (minute == 0) {
            // 使用原始路径
        } else if (minute % 10 == 0) {
            cacheUrl = StrUtil.replaceIgnoreCase(cacheUrl,
                    embyConfig.getOriginPt(), embyConfig.getTransPt1());
        } else {
            if (minute % 3 == 0) {
                cacheUrl = StrUtil.replaceIgnoreCase(cacheUrl,
                        embyConfig.getOriginPt(), embyConfig.getTransPt2());
            } else if (minute % 3 == 1) {
                cacheUrl = StrUtil.replaceIgnoreCase(cacheUrl,
                        embyConfig.getOriginPt(), embyConfig.getTransPt3());
            } else if (minute % 3 == 2) {
                cacheUrl = StrUtil.replaceIgnoreCase(cacheUrl,
                        embyConfig.getOriginPt(), embyConfig.getTransPt4());
            }
        }
        return cacheUrl;
    }

    /**
     * 获取格式化url
     *
     * @param path       路径
     * @param tmdbConfig tmdb配置
     * @return {@link String }
     */
    private String getFormattedUrl(String path, TmdbConfig tmdbConfig, String maxWidth) {
        return StrUtil.isNotBlank(path) ? StrUtil.format(tmdbConfig.getImageCdnUrl(), maxWidth) + path : null;
    }

    /**
     * 处理特殊请求
     * // https://api.themoviedb.org/3/tv/1399/images?api_key=950257e1e6e1b7e0daf665312d82e922
     * // https://static-mdb.v.geilijiasu.com/t/p/w400//9RO2vbQ67otPrBLXCaC8UMp3Qat.png
     *
     * @param request  要求
     * @param response 响应
     */
    @SneakyThrows
    private void processPic(EmbyContentCacheReqWrapper request, HttpServletResponse response, EmbyPicType picType) {
        String mediaSourceIdStr = ReUtil.get(RegexPool.NUMBERS, request.getRequestURI(), 0);
        if (StrUtil.isBlank(mediaSourceIdStr)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Integer mediaSourceId = NumberUtil.parseInt(mediaSourceIdStr);
        EmbyItemPic itemPic = embyItemPicDao.findByItemId(mediaSourceId);
        if (null != itemPic) {
            String maxWidth = MapUtil.getStr(request.getCachedParam(), "maxwidth", "400");
            String url = switch (picType) {
                case 封面 -> getFormattedUrl(itemPic.getPosterPath(), tmdbConfig, maxWidth);
                case 背景图 -> getFormattedUrl(itemPic.getBackdropPath(), tmdbConfig, maxWidth);
                case Logo -> getFormattedUrl(itemPic.getLogoPath(), tmdbConfig, maxWidth);
                default -> throw new BaseException("图片类型异常: " + picType);
            };
            if (StrUtil.isNotBlank(url)) {
                response.setStatus(HttpServletResponse.SC_FOUND);
                response.setHeader("Location", url);
                log.info("{}-图片重定向(DB):[{}] => {}", picType, mediaSourceId, url);
                return;
            }
        }

        // 获取或创建对应的锁
        String lockKey = mediaSourceId + "|" + picType.getValue();
        Lock lock = picLockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());
        if (!lock.tryLock(15, TimeUnit.SECONDS)) {
            log.warn("请求锁被占用: {}", mediaSourceId);
            response.setStatus(429);
            return;
        }
        try {
            exec302Pic(request, response, mediaSourceId, picType);
        } finally {
            lock.unlock();
            picLockMap.remove(lockKey);
        }

    }

    private void exec302Pic(EmbyContentCacheReqWrapper request, HttpServletResponse response,
                            Integer mediaSourceId, EmbyPicType picType) {
        EmbyRemoteImageOut.Img imageInfo = embyProxy.getRemoteImage(mediaSourceId, picType);
        if (null != imageInfo) {
            String originUrl = StrUtil.removePrefixIgnoreCase(imageInfo.getUrl(), tmdbConfig.getImageStaticUrl());
            String url = StrUtil.format(tmdbConfig.getImageCdnUrl(), "400") + originUrl;
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", url);
            log.warn("{}-图片重定向(请求):[{}] => {}", picType, mediaSourceId, url);
            ThreadUtil.execAsync(() -> {
                EmbyItemPic picDto = switch (picType) {
                    case 封面 -> EmbyItemPic.builder().itemId(mediaSourceId).posterPath(originUrl).build();
                    case 背景图 -> EmbyItemPic.builder().itemId(mediaSourceId).backdropPath(originUrl).build();
                    case Logo -> EmbyItemPic.builder().itemId(mediaSourceId).logoPath(originUrl).build();
                    default -> throw new BaseException("图片类型异常: " + picType);
                };
                embyItemPicDao.insertOrUpdate(picDto);
            });
            return;
        }
        forwardOriReq(request, response);
    }

    private void exec302(EmbyContentCacheReqWrapper request,
                         HttpServletResponse response, String mediaSourceId) {
        FileCacheUtil.FileInfo embyInfo = embyUtil.getFileInfo(NumberUtil.parseLong(mediaSourceId));
        if (null == embyInfo) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String mediaPath = embyInfo.getPath();
        Map<String, String> header302 = MapUtil.<String, String>builder()
                .put("User-Agent", request.getUa()).put("range", request.getRange()).build();
        // fileCacheUtil.cacheNextEpisode(embyInfo, header302);
        if (StrUtil.startWithIgnoreCase(mediaPath, "http")) {
            String realUrl = embyUtil.fetch302Path(mediaPath, header302);
            embyInfo.setRealUrl(realUrl);
            if (StrUtil.isBlank(realUrl)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            /*if (findRangeCache(request, response, embyInfo)) {
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                return;
            }*/

            int exTime = 2 * 24 * 60 * 60;
            if (StrUtil.startWithIgnoreCase(realUrl, embyConfig.getOriginPt())) {
                redisClient.set(mediaSourceId, realUrl, exTime);
            } else {
                exTime = (int) (MapUtil.getLong(UrlQueryUtil.decodeQuery(realUrl, Charset.defaultCharset()),
                        "t") - DateUtil.currentSeconds() - 5 * 60);
                redisClient.set(request.getUa() + "|" + mediaSourceId, realUrl, exTime);
            }

            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", realUrl);
            log.info("▲ 请求重定向->[{}-{}] {} 【{}】", response.getStatus(),
                    request.getMethod(), request.getRequestURI(), request.getCachedHeader());
            log.warn("重定向({}):[{}] => {}", DateUtil.date(DateUtil.currentSeconds() + exTime), mediaPath, realUrl);
            return;
        } else {
            // TODO 这里是不需要302的本地路径
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
                ThreadUtil.execAsync(() -> fileCacheUtil.writeCacheFile(embyInfo, request.getCachedHeader()));
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
                ThreadUtil.execAsync(() -> fileCacheUtil.writeCacheFile(embyInfo, request.getCachedHeader()));
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
                ThreadUtil.execAsync(() -> fileCacheUtil.writeCacheFile(embyInfo, request.getCachedHeader()));
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
                    ThreadUtil.execAsync(() -> fileCacheUtil.writeCacheFile(embyInfo, request.getCachedHeader()));
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
                    ThreadUtil.execAsync(() -> fileCacheUtil.writeCacheFile(embyInfo, request.getCachedHeader()));
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

    private String getContentType(String container) {
        return switch (container) {
            case "mp4" -> "video/mp4";
            case "mkv" -> "video/x-matroska";
            case "webm" -> "video/webm";
            case "ogg" -> "video/ogg";
            case "mpeg", "mpg" -> "video/mpeg";
            case "mov" -> "video/quicktime";
            case "ts" -> "video/mp2t";
            case "wmv" -> "video/x-ms-wmv";
            case "avi" -> "video/x-msvideo";
            case "flv" -> "video/x-flv";
            default -> "application/octet-stream";
        };
    }

    /**
     * 转发原始请求
     *
     * @param request  要求
     * @param response 响应
     */
    @SneakyThrows
    private void forwardOriReq(EmbyContentCacheReqWrapper request, HttpServletResponse response) {
        StopWatch stopWatch = StopWatch.of("原始请求");
        String cacheKey = staticCacheKey(request);

        EmbyCachedResp cached = redisClient.getBean(cacheKey);
        if (cached != null) {
            writeCacheResponse(response, cached);
            return;
        }
        cached = new EmbyCachedResp();

        // 获取或创建对应的锁
        stopWatch.start("上锁");
        Lock lock = urlLockMap.computeIfAbsent(cacheKey, k -> new ReentrantLock());
        lock.lock();
        try {
            stopWatch.stop();

            // 原始请求转发
            String url = HttpUtil.urlWithFormUrlEncoded(embyConfig.getHost() + request.getRequestURI(),
                    request.getCachedParam(), Charset.defaultCharset());
            Request originalRequest = Request.of(url).method(Method.valueOf(request.getMethod()))
                    .body(request.getCachedBody())
                    .header(request.getCachedHeader());
            stopWatch.start("转发");
            try (Response res = httpClient.send(originalRequest).sync()) {
                cached = EmbyCachedResp.transfer(res, request.getMethod());
                writeCacheResponse(request, response, cached);
            } catch (Throwable e) {
                if (StrUtil.contains(ExceptionUtil.getSimpleMessage(e), "Cannot invoke " +
                        "\"org.apache.hc.core5.http.HttpEntity.getContent()\" because \"this.entity\" is null")) {
                    // log.warn("转发请求无响应-忽略[{}]: {},e:{}", request.getMethod(), request.getRequestURI(), ExceptionUtil.getSimpleMessage(e));
                    cached.setStatusCode(HttpStatus.HTTP_OK);
                    writeCacheResponse(request, response, cached);
                } else {
                    throw e;
                }
            } finally {
                stopWatch.stop();
                log.info("请求转发->[{}-{}:{}-{}] {}", cached.getStatusCode(), request.getMethod(),
                        stopWatch.getTaskInfo()[0].getTimeMillis(), stopWatch.getTaskInfo()[1].getTimeMillis(),
                        StrUtil.format("{}&api_key={}", url, embyConfig.getApiKey()));
            }
        } finally {
            lock.unlock();
            urlLockMap.remove(cacheKey);
        }
    }

    /**
     * 返回响应(满足条件会缓存)
     *
     * @param request 请求
     * @param res     事件
     * @param cached  高速缓存
     */
    @SneakyThrows
    private void writeCacheResponse(EmbyContentCacheReqWrapper request,
                                    HttpServletResponse res, EmbyCachedResp cached) {
        if (null != request && cached.getStatusCode() == OK && StrUtil.equalsIgnoreCase(request.getMethod(), "GET")) {
            int exTime = 5;
            if (isCacheReq(request)) {
                exTime = 2 * 24 * 60 * 60;
            }
            redisClient.setBean(staticCacheKey(request), cached, exTime);
        }
        res.setStatus(cached.getStatusCode());
        cached.getHeaders().forEach(res::setHeader);
        if (ArrayUtil.isNotEmpty(cached.getContent())) {
            res.getOutputStream().write(cached.getContent());
        }

    }

    /**
     * 返回响应
     *
     * @param res    事件
     * @param cached 高速缓存
     */
    private void writeCacheResponse(HttpServletResponse res, EmbyCachedResp cached) {
        writeCacheResponse(null, res, cached);
    }
}