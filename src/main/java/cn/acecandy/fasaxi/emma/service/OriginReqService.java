package cn.acecandy.fasaxi.emma.service;

import cn.acecandy.fasaxi.emma.common.resp.EmbyCachedResp;
import cn.acecandy.fasaxi.emma.config.EmbyConfig;
import cn.acecandy.fasaxi.emma.config.EmbyContentCacheReqWrapper;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.proxy.EmbyProxy;
import cn.acecandy.fasaxi.emma.utils.CacheUtil;
import cn.acecandy.fasaxi.emma.utils.EmbyProxyUtil;
import cn.acecandy.fasaxi.emma.utils.LockUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.array.ArrayUtil;
import org.dromara.hutool.core.date.StopWatch;
import org.dromara.hutool.core.exception.ExceptionUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.core.thread.ThreadUtil;
import org.dromara.hutool.http.client.Request;
import org.dromara.hutool.http.client.Response;
import org.dromara.hutool.http.client.engine.ClientEngine;
import org.dromara.hutool.http.meta.Method;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.Lock;

import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_200;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_204;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.HTTP_GET;
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

    /**
     * 转发原始请求
     *
     * @param request  要求
     * @param response 响应
     */
    @SneakyThrows
    public void forwardOriReq(EmbyContentCacheReqWrapper request, HttpServletResponse response) {
        StopWatch stopWatch = StopWatch.of("原始请求");
        if (!StrUtil.equalsIgnoreCase(request.getMethod(), HTTP_GET)) {
            try {
                execOriginReq(request, response, stopWatch);
            } finally {
                if (StrUtil.isNotBlank(request.getUserId()) && StrUtil.isNotBlank(request.getMediaSourceId())) {
                    redisClient.delByPrefix(StrUtil.format("/emby/Users/{}/Items/{}",
                            request.getUserId(), request.getMediaSourceId()));
                }
            }
            return;
        }

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
            execOriginReq(request, response, stopWatch);
        } finally {
            LockUtil.unlockOrigin(lock, request);
        }
    }

    private void execOriginReq(EmbyContentCacheReqWrapper request, HttpServletResponse response,
                               StopWatch stopWatch) throws Throwable {
        String cacheKey = CacheUtil.buildOriginCacheKey(request);
        EmbyCachedResp cached = redisClient.getBean(cacheKey);
        if (cached != null) {
            writeCacheResponse(response, cached);
            return;
        }

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
                log.info("请求原始转发->[{}-{}:{}ms] {}", cached.getStatusCode(), request.getMethod(),
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
    private void httpClient5WarningCatch(EmbyContentCacheReqWrapper request, HttpServletResponse response,
                                         Throwable e, EmbyCachedResp cached) throws Throwable {
        if (StrUtil.contains(ExceptionUtil.getSimpleMessage(e), "Cannot invoke " +
                "\"org.apache.hc.core5.http.HttpEntity.getContent()\" because \"this.entity\" is null")) {
            cached.setStatusCode(CODE_204);
            writeCacheResponse(request, response, cached);
        } else {
            throw e;
        }
    }

    /**
     * 发送原始请求
     *
     * @param request 要求
     * @return {@link Response }
     */
    private Response sendOriginReq(EmbyContentCacheReqWrapper request) {
        Request originalRequest = Request.of(embyConfig.getHost() + request.getParamUri())
                .method(Method.valueOf(request.getMethod()))
                .body(request.getCachedBody()).header(request.getCachedHeader());
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
            res.getOutputStream().write(cached.getContent());
        }
    }

    /**
     * 异步写入 实体
     *
     * @param request 请求
     * @param cached  缓存返回
     */
    private void asyncWriteOriginReq(EmbyContentCacheReqWrapper request, EmbyCachedResp cached) {
        ThreadUtil.execAsync(() -> {
            if (null == request) {
                return;
            }
            if (!StrUtil.equalsIgnoreCase(request.getMethod(), HTTP_GET)) {
                return;
            }
            if (!cached.getStatusCode().equals(CODE_200)) {
                return;
            }
            int exTime = 10;
            if (isCacheStaticReq(request)) {
                exTime = 2 * 24 * 60 * 60;
            }
            redisClient.setBean(CacheUtil.buildOriginCacheKey(request), cached, exTime);
        });
    }
}