package cn.acecandy.fasaxi.emma.service;

import cn.acecandy.fasaxi.emma.common.resp.EmbyCachedResp;
import cn.acecandy.fasaxi.emma.config.EmbyConfig;
import cn.acecandy.fasaxi.emma.config.EmbyContentCacheReqWrapper;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.proxy.EmbyProxy;
import cn.acecandy.fasaxi.emma.utils.CacheUtil;
import cn.acecandy.fasaxi.emma.utils.EmbyProxyUtil;
import cn.acecandy.fasaxi.emma.utils.FileCacheUtil;
import cn.acecandy.fasaxi.emma.utils.LockUtil;
import cn.acecandy.fasaxi.emma.utils.ReUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.dromara.hutool.core.array.ArrayUtil;
import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.date.StopWatch;
import org.dromara.hutool.core.exception.ExceptionUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.http.client.Request;
import org.dromara.hutool.http.client.Response;
import org.dromara.hutool.http.client.engine.ClientEngine;
import org.dromara.hutool.http.client.engine.ClientEngineFactory;
import org.dromara.hutool.http.meta.Method;
import org.dromara.hutool.http.server.servlet.ServletUtil;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.Lock;

import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_200;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_204;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_599;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.HTTP_DELETE;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.HTTP_GET;
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
    private FileCacheUtil fileCacheUtil;

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
        Lock lock = LockUtil.lockOrigin(request);
        if (LockUtil.isLock1s(lock)) {
            response.setStatus(CODE_204);
            return;
        }
        try {
            execOriginReq(request, response);
        } finally {
            LockUtil.unlockOrigin(lock, request);
        }
    }

    @SneakyThrows
    public boolean notGetReq(HttpServletResponse response, EmbyContentCacheReqWrapper req) {
        if (StrUtil.equalsIgnoreCase(req.getMethod(), HTTP_GET)) {
            return false;
        }
        try {
            String url = embyConfig.getHost() + req.getParamUri();
            Request originalRequest = Request.of(url).method(Method.valueOf(req.getMethod()))
                    .body(req.getCachedBody());
            ServletUtil.getHeadersMap(req).forEach((k, v) -> {
                originalRequest.header(ReUtil.capitalizeWords(k), CollUtil.getFirst(v));
            });
            try (Response res = ClientEngineFactory.createEngine("OkHttp").send(originalRequest)) {
                response.setStatus(res.getStatus());
                /*res.headers().forEach((k, v) -> {
                    if (StrUtil.equalsAnyIgnoreCase(k, "content-encoding", "content-length")) {
                        return;
                    }
                    response.setHeader(k, CollUtil.getFirst(v));
                });*/
                // ServletUtil.write(response, res.bodyStream());
                try (ServletOutputStream outputStream = response.getOutputStream()) {
                    outputStream.write(res.bodyBytes());
                } catch (ClientAbortException e) {
                    // 客户端中止连接 不做处理
                }
            }
        } finally {
            if (StrUtil.equalsIgnoreCase(req.getMethod(), HTTP_DELETE)) {
                redisClient.delByPrefix(CacheUtil.buildOriginRefreshCacheAllKey(req));
            } else {
                redisClient.delByPrefix(CacheUtil.buildOriginRefreshCacheKey(req));
            }
        }
        return true;
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
            cached = embyProxy.transferResp(res, request);
            writeCacheResponse(request, response, cached);
        } catch (Throwable e) {
            httpClient5WarningCatch(request, response, e, cached);
        } finally {
            stopWatch.stop();
            if (EmbyProxyUtil.isHttpOk(cached.getStatusCode())) {
                log.debug("请求原始转发->[{}-{}:{}ms] {}", cached.getStatusCode(), request.getMethod(),
                        stopWatch.getLastTaskTimeMillis(),
                        StrUtil.format("{}&api_key={}", request.getParamUri(), embyConfig.getApiKey()));
            } else {
                log.error("请求原始转发->[{}-{}:{}ms] {}", cached.getStatusCode(), request.getMethod(),
                        stopWatch.getLastTaskTimeMillis(),
                        StrUtil.format("{}&api_key={}", request.getParamUri(), embyConfig.getApiKey()));
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
        if (StrUtil.contains(ExceptionUtil.getSimpleMessage(e), "Cannot invoke " +
                "\"org.apache.hc.core5.http.HttpEntity.getContent()\" because \"this.entity\" is null")) {
            // log.error("204还是有报错:{}", ExceptionUtil.getSimpleMessage(e));
            cached.setStatusCode(CODE_204);
            writeCacheResponse(response, cached);
        } else {
            cached.setStatusCode(CODE_599);
            log.error("请求599[{}]: {}&api_key={}, e:{}", request.getMethod(), request.getParamUri(), embyConfig.getApiKey(), ExceptionUtil.stacktraceToString(e));
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
        Request originalRequest = Request.of(embyConfig.getHost() + request.getParamUri())
                .method(Method.valueOf(request.getMethod()))
                .body(request.getCachedBody());
        request.getCachedHeader().forEach((k, v) -> originalRequest.header(k, v, true));
        return httpClient.send(originalRequest);
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
            } catch (ClientAbortException e) {
                // 客户端中止连接 不做处理
            }
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
        if (null == request) {
            return;
        }
        if (!StrUtil.equalsIgnoreCase(request.getMethod(), HTTP_GET)) {
            return;
        }
        if (!cached.getStatusCode().equals(CODE_200)) {
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
}