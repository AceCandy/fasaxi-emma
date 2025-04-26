package cn.acecandy.fasaxi.emma.config;

import cn.acecandy.fasaxi.emma.common.resp.EmbyCachedResp;
import cn.acecandy.fasaxi.emma.common.vo.EmbyUrlCacheVO;
import cn.acecandy.fasaxi.emma.utils.EmbyUtil;
import cn.acecandy.fasaxi.emma.utils.FileCacheUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Resource;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

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
    private CloseableHttpClient embyHttpClient;

    @Resource
    private FastEmbyConfig fastEmbyConfig;

    @Resource
    private EmbyUtil embyUtil;

    @Resource
    private FileCacheUtil fileCacheUtil;

    private final Cache<String, EmbyCachedResp> staticCache = Caffeine.newBuilder()
            .maximumSize(1000).expireAfterWrite(12, TimeUnit.HOURS)
            .build();

    private final Cache<String, EmbyUrlCacheVO> urlCache = Caffeine.newBuilder()
            .maximumSize(1000).expireAfterWrite(1, TimeUnit.DAYS)
            .build();

    private final ConcurrentHashMap<String, ReentrantLock> urlLockMap = MapUtil.newConcurrentHashMap();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // 缓存原始请求数据
        EmbyContentCacheReqWrapper reqWrapper = new EmbyContentCacheReqWrapper(req);
        try {
            if (needVideoRedirect(req)) {
                processVideo(reqWrapper, res);
            } else {
                forwardOriReq(reqWrapper, res);
            }
        } catch (Exception e) {
            log.warn("转发请求失败[{}]: {}", req.getMethod(), reqWrapper.getRequestURI(), e);
            forwardOriReq(reqWrapper, res);
        }
    }

    private boolean needVideoRedirect(HttpServletRequest req) {
        return StrUtil.containsAll(req.getRequestURI(), "/emby/videos/", "/original");
    }

    /**
     * 静态资源
     *
     * @param req 请求入参
     * @return boolean
     */
    private boolean isCacheReq(HttpServletRequest req) {
        String uri = req.getRequestURI().toLowerCase();
        if (StrUtil.containsAny(uri, "/images/primary", "/images/backdrop")) {
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
        return StrUtil.format("{}?{}", req.getRequestURI(), req.getCacheParam());
    }

    /**
     * 处理特殊请求
     *
     * @param request  要求
     * @param response 响应
     */
    @SneakyThrows
    private void processVideo(EmbyContentCacheReqWrapper request, HttpServletResponse response) {
        Map<String, Object> params = request.getCacheParam();
        String mediaSourceId = StrUtil.removePrefixIgnoreCase(
                MapUtil.getStr(params, "MediaSourceId"), "mediasource_");
        if (StrUtil.isBlank(mediaSourceId)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 获取或创建对应的锁
        ReentrantLock lock = urlLockMap.computeIfAbsent(mediaSourceId, k -> new ReentrantLock());

        try {
            lock.lock();
            exec302(request, response, mediaSourceId);
        } finally {
            lock.unlock();
            urlLockMap.remove(mediaSourceId);
        }
    }

    private void exec302(EmbyContentCacheReqWrapper request, HttpServletResponse response, String mediaSourceId) {
        Map<String, String> headers = request.getHeaderMap();
        String ua = headers.get("User-Agent");

        String cacheKey = ua + "|" + mediaSourceId;
        EmbyUrlCacheVO urlVO = urlCache.getIfPresent(cacheKey);
        if (null != urlVO && urlVO.isEffect()) {
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", urlVO.getUrl());
            log.warn("重定向(缓存UA+Media):[{}] => {}", cacheKey, urlVO.getUrl());
            return;
        }
        cacheKey = mediaSourceId;
        urlVO = urlCache.getIfPresent(cacheKey);
        if (null != urlVO && urlVO.isEffect()) {
            String url = urlVO.getUrl();
            if (StrUtil.startWithIgnoreCase(url, fastEmbyConfig.getOriginPt())) {
                int minute = DateUtil.thisMinute();
                if (minute == 0) {
                    // 使用原始路径
                } else if (minute % 10 == 0) {
                    url = StrUtil.replaceIgnoreCase(url,
                            fastEmbyConfig.getOriginPt(), fastEmbyConfig.getTransPt1());
                } else {
                    if (minute % 3 == 0) {
                        url = StrUtil.replaceIgnoreCase(url,
                                fastEmbyConfig.getOriginPt(), fastEmbyConfig.getTransPt2());
                    } else if (minute % 3 == 1) {
                        url = StrUtil.replaceIgnoreCase(url,
                                fastEmbyConfig.getOriginPt(), fastEmbyConfig.getTransPt3());
                    } else if (minute % 3 == 2) {
                        url = StrUtil.replaceIgnoreCase(url,
                                fastEmbyConfig.getOriginPt(), fastEmbyConfig.getTransPt4());
                    }
                }
            }

            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", url);
            log.warn("重定向(缓存Media):[{}] => {}", cacheKey, url);
            return;
        }

        FileCacheUtil.FileInfo embyInfo = embyUtil.getFileInfo(NumberUtil.parseLong(mediaSourceId));
        String mediaPath = embyInfo.getPath();
        Map<String, String> header302 = MapUtil.<String, String>builder().put("User-Agent", ua).build();
        fileCacheUtil.cacheNextEpisode(embyInfo, header302);
        if (StrUtil.startWithIgnoreCase(mediaPath, "http")) {
            String realUrl = embyUtil.fetch302Path(mediaPath, header302);
            embyInfo.setRealUrl(realUrl);
            if (StrUtil.isBlank(realUrl)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            rangeCache(request, response, embyInfo);

            long exTime = DateUtil.currentSeconds() + 2 * 24 * 60 * 60;
            if (StrUtil.startWithIgnoreCase(realUrl, fastEmbyConfig.getOriginPt())) {
                urlCache.put(mediaSourceId, EmbyUrlCacheVO.builder().url(realUrl)
                        .exTime(exTime).build());
            } else {
                Map<String, String> paramMap = HttpUtil.decodeParamMap(realUrl, Charset.defaultCharset());
                exTime = MapUtil.getLong(paramMap, "t", exTime) - 5 * 60;
                urlCache.put(ua + "|" + mediaSourceId, EmbyUrlCacheVO.builder().url(realUrl)
                        .exTime(exTime).build());
            }

            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", realUrl);
            log.info("▲ 请求重定向->[{}-{}] {} 【{}】", response.getStatus(),
                    request.getMethod(), request.getRequestURI(), request.getCacheParam());
            log.warn("重定向({}):[{}] => {}", DateUtil.date(exTime * 1000), mediaPath, realUrl);
            return;
        } else {
            // TODO 这里是不需要302的本地路径
        }
    }

    @SneakyThrows
    private void rangeCache(EmbyContentCacheReqWrapper request, HttpServletResponse response,
                            FileCacheUtil.FileInfo embyInfo) {
        String range = request.getHeader("Range");
        if (!StrUtil.startWithIgnoreCase(range, "bytes=")) {
            log.warn("范围标头非法: {}", request.getHeaderMap());
            embyInfo.setStartByte(0L);
            embyInfo.setCacheStatus(FileCacheUtil.CacheStatus.PARTIAL);
            if (fileCacheUtil.checkCacheFile(embyInfo)) {
                response.setHeader("Cache-Control", "private, no-transform, no-cache");
                response.setHeader("Content-Length", String.valueOf(embyInfo.getSize()));
                response.setHeader("X-EmbyToAList-Cache", "Hit");

                Map<String, String> headers = request.getHeaderMap();
                headers.put("Range", StrUtil.format("bytes={}-", embyInfo.getCacheFileSize()));
                mixFileCache(embyInfo, headers);
                return;
            } else {
                ThreadUtil.execAsync(() -> fileCacheUtil.writeCacheFile(embyInfo, request.getHeaderMap()));
                response.setStatus(HttpServletResponse.SC_FOUND);
                response.setHeader("Location", embyInfo.getRealUrl());
                return;
            }
        } else {
            String byteRange = CollUtil.getLast(StrUtil.split(range, "="));
            List<String> rangeList = StrUtil.split(byteRange, "-");
            Long startByte = NumberUtil.parseLong(CollUtil.getFirst(rangeList), null);
            Long endByte = NumberUtil.parseLong(CollUtil.getLast(rangeList), null);
            embyInfo.setStartByte(startByte);
            embyInfo.setEndByte(endByte);
            if (startByte >= embyInfo.getSize()) {
                log.warn("range请求超出文件大小[{}]: {}", embyInfo.getSize(), request.getHeaderMap());
                response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return;
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
                        Map<String, String> headers = request.getHeaderMap();
                        headers.put("Range", sourceRange);
                        mixFileCache(embyInfo, headers);
                        return;
                    } else if (StrUtil.equalsAny(embyInfo.getCacheStatus().name(),
                            FileCacheUtil.CacheStatus.HIT.name(), FileCacheUtil.CacheStatus.HIT_TAIL.name())) {
                        // 完全在范围内
                        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                        mixFileCache(embyInfo, request.getHeaderMap());
                        return;
                    } else {
                        // 请求的范围与缓存重叠并超出缓存
                        Long sourceStart = cacheFileSize;
                        if (endByte != null) {
                            sourceRange = StrUtil.format("bytes={}-{}", sourceStart, endByte - 1);
                        } else {
                            sourceRange = StrUtil.format("bytes={}", sourceStart);
                        }
                        Map<String, String> headers = request.getHeaderMap();
                        headers.put("Range", sourceRange);
                        mixFileCache(embyInfo, headers);
                        return;
                    }
                } else {
                    ThreadUtil.execAsync(() -> fileCacheUtil.writeCacheFile(embyInfo, request.getHeaderMap()));
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
                    mixFileCache(embyInfo, request.getHeaderMap());
                } else {
                    ThreadUtil.execAsync(() -> fileCacheUtil.writeCacheFile(embyInfo, request.getHeaderMap()));
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
                    Map<String, String> headers = request.getHeaderMap();
                    headers.put("Range", sourceRange);
                    mixFileCache(embyInfo, headers);
                    return;
                }
            }
        }
    }

    @SneakyThrows
    private void mixFileCache(FileCacheUtil.FileInfo embyInfo, Map<String, String> headers) {
        Stream<byte[]> cache = fileCacheUtil.readCacheFile(embyInfo);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        cache.parallel().forEach(bytes -> {
            synchronized (outputStream) {  // 保证线程安全
                outputStream.write(bytes, 0, bytes.length);
            }
        });
        log.info("缓存已用尽");


        // request.getHeaderMap().put("host", URI.create(urlTask).getHost());
        try (HttpResponse res = HttpUtil.createGet(embyInfo.getRealUrl())
                .headerMap(headers, true).execute()) {
            outputStream.write(res.bodyBytes());
        }
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
        String cacheKey = staticCacheKey(request);

        // 获取或创建对应的锁
        ReentrantLock lock = urlLockMap.computeIfAbsent(cacheKey, k -> new ReentrantLock());

        try {
            lock.lock();

            EmbyCachedResp cached = staticCache.getIfPresent(cacheKey);
            if (cached != null && cached.getExTime() > DateUtil.currentSeconds()) {
                writeCacheResponse(response, cached);
                return;
            }

            // 原始请求转发
            String url = HttpUtil.urlWithFormUrlEncoded(fastEmbyConfig.getHost() + request.getRequestURI(),
                    request.getCacheParam(), Charset.defaultCharset());
            HttpUriRequest originalRequest = RequestBuilder.create(request.getMethod())
                    .setUri(url)
                    .setEntity(new ByteArrayEntity(request.getContentAsByteArray()))
                    .build();
            request.getHeaderMap().forEach(originalRequest::addHeader);
            originalRequest.removeHeaders("Transfer-Encoding");
            // log.info("请求头:{}=>{}", request.getHeaderMap(), originalRequest.getAllHeaders());

            try (CloseableHttpResponse embyResponse = embyHttpClient.execute(originalRequest)) {
                cached = EmbyCachedResp.transfer(embyResponse);
                log.info("请求转发->[{}-{}] {}", cached.getStatusCode(), request.getMethod(), url);
                writeCacheResponse(request, response, cached);
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
    private void writeCacheResponse(EmbyContentCacheReqWrapper request, HttpServletResponse res, EmbyCachedResp
            cached) {
        if (null != request && cached.getStatusCode() == OK && StrUtil.equalsIgnoreCase(request.getMethod(), "GET")) {
            if (isCacheReq(request)) {
                cached.setExTime(DateUtil.currentSeconds() + 2 * 24 * 60 * 60);
            } else {
                cached.setExTime(DateUtil.currentSeconds() + 5);
            }
            staticCache.put(staticCacheKey(request), cached);
        }
        res.setStatus(cached.getStatusCode());
        cached.getHeaders().forEach(res::setHeader);
        if (null != cached.getContent()) {
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

    public static void main(String[] args) {
        /*String s = "http://x.acecandy.cn:25888/emby/Users/authenticatebyname?X-Emby-Device-Id=87f46ff4-3982-405f-b4d8-ccf42a08c212&X-Emby-Language=zh-cn&Username=ccc&X-Emby-Device-Name=Microsoft%20Edge%20macOS&X-Emby-Token=d20656cfaf524fd58fbfc801627009c7&X-Emby-Client-Version=4.9.0.42&Pw=065975&X-Emby-Client=Emby%20Web";
        Map<String, Object> p = MapUtil.newHashMap();
        p.put("Pw", "065975");
        p.put("Username", "ccc");
        String result2 = HttpRequest.post(s)
                // .header("Access-Control-Allow-Origin", "http://x.acecandy.cn:25888")
                // .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                // .header("Origin", "http://x.acecandy.cn:25888")
                .header("User-Agent", "Yamby/1.0")
                // .body("Username=ccc&Pw=065975")
                .execute().body();
        Console.log(result2);*/

        Console.log(HttpUtil.createRequest(Method.HEAD, "https://alist.acecandy.cn:880/d/pt/Emby1/%E5%8A%A8%E6%BC%AB/2.5%E6%AC%A1%E5%85%83%E7%9A%84%E8%AF%B1%E6%83%91%20(2024)/Season%201/2.5%E6%AC%A1%E5%85%83%E7%9A%84%E8%AF%B1%E6%83%91%20-%20S01E13%20-%20%E7%AC%AC13%E9%9B%86%20-%20ANi.mp4").execute().headers());
    }
}