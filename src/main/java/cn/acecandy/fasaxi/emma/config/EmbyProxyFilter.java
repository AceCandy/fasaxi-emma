package cn.acecandy.fasaxi.emma.config;

import cn.acecandy.fasaxi.emma.common.resp.EmbyCachedResp;
import cn.acecandy.fasaxi.emma.common.vo.EmbyUrlCacheVO;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.alibaba.fastjson2.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Resource;
import jakarta.servlet.*;
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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static io.undertow.util.StatusCodes.FOUND;
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
            log.warn("转发请求失败: {}", reqWrapper.getRequestURI(), e);
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
        if (null != urlVO && urlVO.getExTime() > DateUtil.currentSeconds()) {
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", urlVO.getUrl());
            log.warn("重定向(缓存UA+Media):[{}] => {}", cacheKey, urlVO.getUrl());
            return;
        }
        cacheKey = mediaSourceId;
        urlVO = urlCache.getIfPresent(cacheKey);
        if (null != urlVO && urlVO.getExTime() > DateUtil.currentSeconds()) {
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

        String apiKey = MapUtil.getStr(request.getCacheParam(), "api_key");
        String result = HttpUtil.createGet(fastEmbyConfig.getHost() + "/Items")
                .form("Ids", mediaSourceId)
                .form("Fields", "Path,MediaSources").form("api_key", apiKey)
                .execute().body();
        JSONObject resultJn = JSONObject.parseObject(result);
        JSONObject items = resultJn.getJSONArray("Items").getJSONObject(0);
        String filePath = items.getString("Path");
        String mediaPath = items.getJSONArray("MediaSources").getJSONObject(0).getString("Path");

        if (StrUtil.equalsIgnoreCase(FileUtil.extName(filePath), "strm")) {
            // 外网转为内网
            mediaPath = StrUtil.replaceIgnoreCase(mediaPath,
                    fastEmbyConfig.getAlistPublic(), fastEmbyConfig.getAlistInner());
            try (HttpResponse mediaResp = HttpUtil.createRequest(Method.HEAD, mediaPath).header("User-Agent", ua).execute()) {
                if (mediaResp.getStatus() == FOUND) {
                    String location = mediaResp.header("Location");

                    long exTime = DateUtil.currentSeconds() + 2 * 24 * 60 * 60;
                    if (StrUtil.startWithIgnoreCase(location, fastEmbyConfig.getOriginPt())) {
                        urlCache.put(mediaSourceId, EmbyUrlCacheVO.builder().url(location)
                                .exTime(exTime).build());
                    } else {
                        Map<String, String> paramMap = HttpUtil.decodeParamMap(location, Charset.defaultCharset());
                        exTime = MapUtil.getLong(paramMap, "t", exTime) - 5 * 60;
                        urlCache.put(ua + "|" + mediaSourceId, EmbyUrlCacheVO.builder().url(location)
                                .exTime(exTime).build());
                    }

                    response.setStatus(HttpServletResponse.SC_FOUND);
                    response.setHeader("Location", location);
                    log.warn("重定向({}):[{}] => {}", DateUtil.date(exTime * 1000), mediaPath, location);
                    return;
                }
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }
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
     * @param res      事件
     * @param cached   高速缓存
     */
    @SneakyThrows
    private void writeCacheResponse(EmbyContentCacheReqWrapper request, HttpServletResponse res, EmbyCachedResp cached) {
        if (null != request && cached.getStatusCode() == OK && StrUtil.equalsIgnoreCase(request.getMethod(), "GET")) {
            if (isCacheReq(request)) {
                cached.setExTime(DateUtil.currentSeconds() + 2 * 24 * 60 * 60);
            } else{
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