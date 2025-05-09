package cn.acecandy.fasaxi.emma.service;

import cn.acecandy.fasaxi.emma.config.EmbyConfig;
import cn.acecandy.fasaxi.emma.config.EmbyContentCacheReqWrapper;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.out.EmbyItem;
import cn.acecandy.fasaxi.emma.sao.proxy.EmbyProxy;
import cn.acecandy.fasaxi.emma.utils.CacheUtil;
import cn.acecandy.fasaxi.emma.utils.LockUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.date.DateUtil;
import org.dromara.hutool.core.lang.Console;
import org.dromara.hutool.core.map.MapUtil;
import org.dromara.hutool.core.net.url.UrlQueryUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.http.client.Request;
import org.dromara.hutool.http.client.Response;
import org.dromara.hutool.http.client.engine.ClientEngine;
import org.dromara.hutool.http.meta.Method;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_204;

/**
 * 视频重定向服务
 *
 * @author AceCandy
 * @since 2024/11/19
 */
@Slf4j
@Component
public class VideoRedirectService {

    @Resource
    private OriginReqService originReqService;

    @Resource
    private ClientEngine httpClient;

    @Resource
    private RedisClient redisClient;

    @Resource
    private EmbyConfig embyConfig;

    @Resource
    private EmbyProxy embyProxy;

    @SneakyThrows
    public void processVideo(EmbyContentCacheReqWrapper request, HttpServletResponse response) {
        String mediaSourceId = request.getMediaSourceId();
        if (StrUtil.isBlank(mediaSourceId)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (1 + 1 == 2) {
            originalVideoStream(request, response);
            return;
        }

        String ua = request.getUa();
        if (getByCache(response, mediaSourceId, ua)) {
            return;
        }

        // 获取或创建对应的锁
        Lock lock = LockUtil.lockVideo(mediaSourceId);
        if (LockUtil.isLock1s(lock)) {
            response.setStatus(CODE_204);
            return;
        }
        try {
            if (getByCache(response, mediaSourceId, ua)) {
                return;
            }

            exec302(request, response, mediaSourceId);
        } finally {
            LockUtil.unlockVideo(lock, mediaSourceId);
        }
    }

    private boolean getByCache(HttpServletResponse response, String mediaSourceId, String ua) {
        String cacheUrl = redisClient.getStrFindOne(CacheUtil.buildVideoCacheKeyList(mediaSourceId, ua));
        if (StrUtil.isNotBlank(cacheUrl)) {
            cacheUrl = getPtUrl(cacheUrl);
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", cacheUrl);
            log.warn("重定向(缓存):[{}|{}] => {}", mediaSourceId, ua, cacheUrl);
            return true;
        }
        return false;
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

    private void exec302(EmbyContentCacheReqWrapper request,
                         HttpServletResponse response, String mediaSourceId) {
        EmbyItem itemInfo = embyProxy.getItemInfo(mediaSourceId);
        if (null == itemInfo) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String mediaPath = CollUtil.getFirst(itemInfo.getMediaSources()).getPath();
        Map<String, String> header302 = MapUtil.<String, String>builder()
                .put("User-Agent", request.getUa()).put("range", request.getRange()).build();
        // fileCacheUtil.cacheNextEpisode(embyInfo, header302);
        if (StrUtil.startWithIgnoreCase(mediaPath, "http")) {
            String realUrl = embyProxy.fetch302Path(mediaPath, header302);
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
                redisClient.set(CacheUtil.buildVideoCacheKey(mediaSourceId), realUrl, exTime);
            } else {
                exTime = (int) (MapUtil.getLong(UrlQueryUtil.decodeQuery(realUrl, Charset.defaultCharset()),
                        "t") - DateUtil.currentSeconds() - 5 * 60);
                redisClient.set(CacheUtil.buildVideoCacheKey(mediaSourceId, request.getUa()), realUrl, exTime);
            }

            realUrl = getPtUrl(realUrl);
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", realUrl);
            log.info("▲ 请求重定向->[{}-{}] {} 【{}】", response.getStatus(),
                    request.getMethod(), request.getRequestURI(), request.getCachedHeader());
            log.warn("重定向({}):[{}] => {}", DateUtil.date(DateUtil.currentSeconds() + exTime), mediaPath, realUrl);
            return;
        } else {
            // TODO 这里是不需要302的本地路径
            originalVideoStream(request, response);
        }
    }

    private void originalVideoStream(EmbyContentCacheReqWrapper request, HttpServletResponse response) {
        Request originalRequest = Request.of(embyConfig.getHost() + request.getParamUri())
                .method(Method.valueOf(request.getMethod()))
                .body(request.getCachedBody()).header(request.getCachedHeader());
        if(StrUtil.isBlank(request.getRange()) || StrUtil.equalsIgnoreCase(request.getRange(),"bytes=0-")) {
            originalRequest.header("range", "bytes=0-"+5242879);
        } else if(StrUtil.equalsIgnoreCase(request.getRange(),"bytes=32768-")) {
            originalRequest.header("range", "bytes=32768-"+(5242879+32768));
        } else if(StrUtil.equalsIgnoreCase(request.getRange(),"bytes=392069120-")) {
            originalRequest.header("range", "bytes=392069120-"+(5242879+392069120));
        } else {
            originalRequest.header("range", "byte=791183360-");
        }
        try (Response res = httpClient.send(originalRequest)) {
            response.setStatus(res.getStatus());
            res.headers().forEach((name, values) ->
                    response.setHeader(name, String.join(",", values)));
            Console.log(res.headers());
            response.setHeader("Accept-Ranges", "bytes");


            // 使用虚拟线程池（非阻塞模式）
            try (
                    ServletOutputStream out = response.getOutputStream();
                    InputStream bodyStream = res.bodyStream(); // 确保原始流关闭
                    ReadableByteChannel inChannel = Channels.newChannel(bodyStream);
                    WritableByteChannel outChannel = Channels.newChannel(out)
            ) {
                // 使用直接缓冲区提升性能（适用于大文件）
                ByteBuffer buffer = ByteBuffer.allocateDirect(8192);
                while (inChannel.read(buffer) != -1) {
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        outChannel.write(buffer);
                    }
                    buffer.clear();

                    // 响应客户端中断（关键！）
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException("Streaming interrupted");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // 恢复中断状态
                throw new RuntimeException("Video streaming interrupted", e);
            }
        } catch (IOException e) {
            if (!response.isCommitted()) {
                try {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt(); // 恢复中断状态
            throw new RuntimeException("Thread interrupted", e);
        }
    }

    private void handleStreamError(Throwable ex, HttpServletResponse response) {
        if (!response.isCommitted()) {
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (IOException e) {
                // 记录日志但不再抛出
            }
        } else {
            // 记录流传输中断日志
        }
        if (ex instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
    }
}