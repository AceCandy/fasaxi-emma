package cn.acecandy.fasaxi.emma.service;

import cn.acecandy.fasaxi.emma.common.resp.EmbyCachedResp;
import cn.acecandy.fasaxi.emma.config.EmbyConfig;
import cn.acecandy.fasaxi.emma.config.EmbyContentCacheReqWrapper;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.client.RedisLockClient;
import cn.acecandy.fasaxi.emma.sao.proxy.EmbyProxy;
import cn.acecandy.fasaxi.emma.utils.CacheUtil;
import cn.acecandy.fasaxi.emma.utils.EmbyProxyUtil;
import cn.acecandy.fasaxi.emma.utils.ExceptUtil;
import cn.acecandy.fasaxi.emma.utils.LogSanitizer;
import cn.acecandy.fasaxi.emma.utils.ThreadLimitUtil;
import cn.acecandy.fasaxi.emma.utils.ThreadUtil;
import cn.hutool.v7.core.array.ArrayUtil;
import cn.hutool.v7.core.date.StopWatch;
import cn.hutool.v7.core.exception.ExceptionUtil;
import cn.hutool.v7.core.net.url.UrlUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.core.util.CharsetUtil;
import cn.hutool.v7.http.client.Request;
import cn.hutool.v7.http.client.Response;
import cn.hutool.v7.http.client.engine.ClientEngine;
import cn.hutool.v7.http.meta.Method;
import cn.hutool.v7.http.server.servlet.ServletUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.brotli.dec.BrotliInputStream;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_200;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_204;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_308;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_408;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_599;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.HTTP_DELETE;
import static cn.acecandy.fasaxi.emma.sao.client.RedisLockClient.buildOriginLock;
import static cn.acecandy.fasaxi.emma.sao.client.RedisLockClient.buildSessionsLock;
import static cn.acecandy.fasaxi.emma.utils.EmbyProxyUtil.isCacheLongTimeReq;
import static cn.acecandy.fasaxi.emma.utils.EmbyProxyUtil.isCacheStaticReq;

/**
 * 透明转发服务
 *
 * @author AceCandy
 * @since 2024/11/19
 */
@Slf4j
@Component
public class OriginReqService {

    @Resource
    private EmbyConfig embyConfig;

    @Resource
    private RedisClient redisClient;

    @Resource
    private ClientEngine httpClient;

    @Resource
    private EmbyProxy embyProxy;

    @Resource
    private RedisLockClient redisLockClient;
    @Resource
    private ThreadLimitUtil threadLimitUtil;

    /**
     * 转发原始请求
     *
     * @param request  要求
     * @param response 响应
     */
    @SneakyThrows
    public void forwardOriReq(EmbyContentCacheReqWrapper request, HttpServletResponse response) {
        String cacheKey = CacheUtil.buildOriginCacheKey(request);
        EmbyCachedResp cached = redisClient.getBean(cacheKey);
        if (cached != null) {
            writeCacheResponse(response, cached);
            return;
        }

        // 获取或创建对应的锁
        String lockKey = buildOriginLock(request);
        if (!redisLockClient.lock(lockKey)) {
            response.setStatus(CODE_204);
            return;
        }
        try {
            execOriginReq(request, response);
        } finally {
            redisLockClient.unlock(lockKey);
        }
    }

    @SneakyThrows
    public boolean notGetReq(HttpServletResponse response, EmbyContentCacheReqWrapper req) {
        if (ServletUtil.isGetMethod(req)) {
            return false;
        }
        stopPlay(req);

        if (isPlayingProgressReq(req)) {
            forwardPlayingProgressAsync(req);
            response.setStatus(CODE_204);
            return true;
        }
        try {
            try (Response res = httpClient.send(buildNonGetOriginRequest(req))) {
                writeJsonOriginResponse(response, res);
            }
        } finally {
            clearCache(req);
        }
        return true;
    }

    private boolean isPlayingProgressReq(EmbyContentCacheReqWrapper req) {
        return StrUtil.containsIgnoreCase(req.getParamUri(), "Sessions/Playing/Progress");
    }

    private void forwardPlayingProgressAsync(EmbyContentCacheReqWrapper req) {
        if (redisLockClient.lock(buildSessionsLock(req), 15)) {
            ThreadUtil.execute(() -> httpClient.send(buildPlayingProgressRequest(req)));
        }
    }

    private Request buildPlayingProgressRequest(EmbyContentCacheReqWrapper req) {
        return Request.of(buildOriginUrl(req, true))
                .method(Method.valueOf(req.getMethod()))
                .body(req.getCachedBody());
    }

    private Request buildNonGetOriginRequest(EmbyContentCacheReqWrapper req) {
        return Request.of(buildOriginUrl(req, false))
                .method(Method.valueOf(req.getMethod()))
                .body(req.getCachedBody())
                .header(req.getCachedHeader());
    }

    private String buildOriginUrl(EmbyContentCacheReqWrapper req, boolean appendBlankApiKey) {
        String url = embyConfig.getHost() + req.getParamUri();
        if (appendBlankApiKey || StrUtil.isNotBlank(req.getApiKey())) {
            String apiKey = "api_key=" + req.getApiKey();
            url = UrlUtil.urlWithForm(url, apiKey, CharsetUtil.defaultCharset(), false);
        }
        return url;
    }

    private void writeJsonOriginResponse(HttpServletResponse response, Response res) throws IOException {
        response.setStatus(res.getStatus());
        if (!isJsonResponse(res)) {
            return;
        }

        copyAllowedHeaders(response, res);
        writeOriginBody(response, readOriginBody(res));
    }

    private void copyAllowedHeaders(HttpServletResponse response, Response res) {
        res.headers().forEach((k, v) -> {
            if (EmbyProxyUtil.isNotAllowedHeader(k)) return;
            for (String value : v) response.addHeader(k, value);
        });
    }

    private byte[] readOriginBody(Response res) throws IOException {
        if (StrUtil.equalsIgnoreCase(res.header("Content-Encoding"), "br")) {
            return (new BrotliInputStream(res.bodyStream())).readAllBytes();
        }
        return res.bodyBytes();
    }

    private void writeOriginBody(HttpServletResponse response, byte[] data) throws IOException {
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            outputStream.write(data);
        } catch (IOException e) {
            if (!ExceptUtil.isConnectionTerminated(e)) throw e;
        }
    }

    /**
     * 提取的缓存清理方法
     */
    private void clearCache(EmbyContentCacheReqWrapper req) {
        if (StrUtil.equalsIgnoreCase(req.getMethod(), HTTP_DELETE)) {
            redisClient.delByPrefix(CacheUtil.buildOriginRefreshCacheAllKey(req));
        } else {
            redisClient.delByPrefix(CacheUtil.buildOriginRefreshCacheKey(req));
        }
    }

    public void stopPlay(EmbyContentCacheReqWrapper req) {
        if (!StrUtil.containsIgnoreCase(req.getRequestURI(), "Sessions/Playing/Stopped")) {
            return;
        }
        threadLimitUtil.removeThreadCache(req.getDeviceId());
    }

    private void execOriginReq(EmbyContentCacheReqWrapper request,
                               HttpServletResponse response) throws Throwable {
        String cacheKey = CacheUtil.buildOriginCacheKey(request);
        EmbyCachedResp cached = redisClient.getBean(cacheKey);
        if (cached != null) {
            writeCacheResponse(response, cached);
            return;
        }

        StopWatch stopWatch = StopWatch.of("原始请求");
        stopWatch.start("转发");
        cached = new EmbyCachedResp();
        try (Response res = sendOriginReq(request)) {
            if (writeStreamResponseIfNeeded(request, response, res)) {
                cached.setStatusCode(res.getStatus());
                return;
            }
            cached = embyProxy.transferResp(res, request);
            writeCacheResponse(request, response, cached);
        } catch (Throwable e) {
            httpClient5WarningCatch(request, response, e, cached);
        } finally {
            stopWatch.stop();
            if (EmbyProxyUtil.isHttpOk2(cached.getStatusCode())) {
                log.debug("请求原始转发->[{}-{}:{}ms] {}", cached.getStatusCode(), request.getMethod(),
                        stopWatch.getLastTaskTimeMillis(),
                        LogSanitizer.sanitizeUri(request.getParamUri()));
            } else {
                log.error("请求原始转发->[{}-{}:{}ms] {}", cached.getStatusCode(), request.getMethod(),
                        stopWatch.getLastTaskTimeMillis(),
                        LogSanitizer.sanitizeUri(request.getParamUri()));
            }
        }
    }

    /**
     * hutool 使用httpClient5 无法处理返回为空的情况 先手动catch
     *
     * @param request  要求
     * @param response 响应
     * @param e        e
     * @param cached   高速缓存
     * @throws Throwable 可抛出
     */
    private void httpClient5WarningCatch(EmbyContentCacheReqWrapper request,
                                         HttpServletResponse response,
                                         Throwable e, EmbyCachedResp cached) throws Throwable {
        String exception = ExceptionUtil.getSimpleMessage(e);
        if (StrUtil.contains(exception, "Cannot invoke " +
                "\"org.apache.hc.core5.http.HttpEntity.getContent()\" because \"this.entity\" is null")) {
            log.error("204还是有报错:{}", ExceptionUtil.getSimpleMessage(e));
            cached.setStatusCode(CODE_204);
            writeCacheResponse(response, cached);
        } else if (StrUtil.contains(exception, "SocketTimeoutException: Read timed out")) {
            cached.setStatusCode(CODE_408);
            throw e;
        } else {
            cached.setStatusCode(CODE_599);
            log.error("请求599[{}]: {}, e:{}", request.getMethod(),
                    LogSanitizer.sanitizeUri(request.getParamUri()), ExceptionUtil.stacktraceToString(e));
            throw e;
        }
    }

    /**
     * 发送原始请求
     *
     * @param request 要求
     * @return {@link Response }
     */
    public Response sendOriginReq(EmbyContentCacheReqWrapper request) {
        String host = embyConfig.getHost();
        String uri = request.getParamUri();
        // String apiKey = "api_key=" + embyConfig.getApiKey();
        // uri = HttpUtil.urlWithForm(host + uri, apiKey, CharsetUtil.defaultCharset(), false);
        Request originalRequest = Request.of(host + uri)
                .method(Method.valueOf(request.getMethod()))
                .body(request.getCachedBody()).header(request.getCachedHeader());
        return httpClient.send(originalRequest);
    }

    private boolean writeStreamResponseIfNeeded(EmbyContentCacheReqWrapper request,
                                                HttpServletResponse response,
                                                Response originResponse) throws IOException {
        if (!embyConfig.isStreamNonJsonOrigin()) {
            return false;
        }
        if (!ServletUtil.isGetMethod(request) || !originResponse.isOk()
                || originResponse.getStatus() == CODE_204 || isJsonResponse(originResponse)) {
            return false;
        }

        response.setStatus(originResponse.getStatus());
        copyAllowedHeaders(response, originResponse);

        try (InputStream inputStream = originResponse.bodyStream();
             ServletOutputStream outputStream = response.getOutputStream()) {
            if (inputStream != null) {
                inputStream.transferTo(outputStream);
            }
        } catch (IOException e) {
            if (!ExceptUtil.isConnectionTerminated(e)) {
                throw e;
            }
        }
        return true;
    }

    private boolean isJsonResponse(Response res) {
        return StrUtil.containsIgnoreCase(res.header("Content-Type"), "application/json");
    }

    /**
     * 返回响应(满足条件会缓存)
     *
     * @param request 请求
     * @param res     事件
     * @param cached  高速缓存
     */

    private void writeCacheResponse(EmbyContentCacheReqWrapper request,
                                    HttpServletResponse res, EmbyCachedResp cached) {
        writeCacheResponse(res, cached);
        asyncWriteOriginReq(request, cached);
    }

    /**
     * 返回响应
     *
     * @param res    事件
     * @param cached 高速缓存
     */
    @SneakyThrows
    private void writeCacheResponse(HttpServletResponse res, EmbyCachedResp cached) {
        res.setStatus(cached.getStatusCode());
        cached.getHeaders().forEach(res::setHeader);
        if (ArrayUtil.isNotEmpty(cached.getContent())) {
            try (ServletOutputStream outputStream = res.getOutputStream()) {
                outputStream.write(cached.getContent());
            } catch (IOException e) {
                if (!ExceptUtil.isConnectionTerminated(e)) {
                    throw e;
                }
            }
        }
    }

    public void return308(HttpServletResponse response, String url) {
        response.setStatus(CODE_308);
        response.setHeader("Location", url);
        response.setHeader("Referer", UrlUtil.url(url).getHost());
    }

    public void return308to200(HttpServletResponse response, String url) {
        try (Response res = httpClient.send(Request.of(embyConfig.getHost() + url).method(Method.GET))) {
            response.setStatus(res.getStatus());
            ServletUtil.write(response, res.bodyStream(), res.header("Content-Type"));
        } catch (Exception e) {
            return308(response, embyConfig.getOuterHost() + url);
        }
    }


    /**
     * 异步写入 实体
     *
     * @param request 请求
     * @param cached  缓存返回
     */
    private void asyncWriteOriginReq(EmbyContentCacheReqWrapper request,
                                     EmbyCachedResp cached) {
        if (!isOriginResponseCacheable(request, cached)) {
            return;
        }
        int exTime = 5;
        if (isCacheStaticReq(request)) {
            exTime = 2 * 24 * 60 * 60;
        }
        if (isCacheLongTimeReq(request)) {
            exTime = 3 * 60 * 60;
        }
        redisClient.setBean(CacheUtil.buildOriginCacheKey(request), cached, exTime);
    }

    boolean isOriginResponseCacheable(EmbyContentCacheReqWrapper request, EmbyCachedResp cached) {
        if (null == request || null == cached) {
            return false;
        }
        if (!ServletUtil.isGetMethod(request)) {
            return false;
        }
        if (!Integer.valueOf(CODE_200).equals(cached.getStatusCode())) {
            return false;
        }
        byte[] content = cached.getContent();
        int maxBodyBytes = embyConfig.getOriginCacheMaxBodyBytes();
        return maxBodyBytes <= 0 || content == null || content.length <= maxBodyBytes;
    }

}
