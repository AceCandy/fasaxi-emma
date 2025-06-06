package cn.acecandy.fasaxi.emma.service;

import cn.acecandy.fasaxi.emma.config.EmbyConfig;
import cn.acecandy.fasaxi.emma.config.EmbyContentCacheReqWrapper;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.out.EmbyItem;
import cn.acecandy.fasaxi.emma.sao.proxy.EmbyProxy;
import cn.acecandy.fasaxi.emma.utils.CacheUtil;
import cn.acecandy.fasaxi.emma.utils.EmbyProxyUtil;
import cn.acecandy.fasaxi.emma.utils.FileCacheUtil;
import cn.acecandy.fasaxi.emma.utils.IpUtil;
import cn.acecandy.fasaxi.emma.utils.LockUtil;
import cn.acecandy.fasaxi.emma.utils.ThreadUtil;
import cn.acecandy.fasaxi.emma.utils.VideoUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.date.DateUtil;
import org.dromara.hutool.core.lang.Console;
import org.dromara.hutool.core.map.MapUtil;
import org.dromara.hutool.core.net.url.UrlDecoder;
import org.dromara.hutool.core.net.url.UrlEncoder;
import org.dromara.hutool.core.net.url.UrlQueryUtil;
import org.dromara.hutool.core.net.url.UrlUtil;
import org.dromara.hutool.core.text.StrPool;
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
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_204;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_404;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_416;

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
    private VideoUtil videoUtil;

    @Resource
    private ClientEngine httpClient;

    @Resource
    private RedisClient redisClient;

    @Resource
    private EmbyConfig embyConfig;

    @Resource
    private EmbyProxy embyProxy;

    @Resource
    private FileCacheUtil fileCacheUtil;

    @SneakyThrows
    public void processVideo(EmbyContentCacheReqWrapper request, HttpServletResponse response) {
        String mediaSourceId = request.getMediaSourceId();
        if (StrUtil.isBlank(mediaSourceId)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        EmbyItem embyItem = embyProxy.getItemInfo(mediaSourceId);
        if (IpUtil.isInnerIp(request.getIp())) {
            if (null == embyItem) {
                response.setStatus(CODE_404);
                return;
            }

            EmbyProxyUtil.Range range = EmbyProxyUtil.parseRangeHeader(request.getRange(), embyItem.getSize());
            if (null == range) {
                response.setHeader("Content-Range", "bytes */" + embyItem.getSize());
                response.setStatus(CODE_416);
                return;
            }
            if (fileCacheUtil.readFile(response, embyItem, range)) {
                return;
            }
        }
        if (StrUtil.containsIgnoreCase(embyItem.getPath(), "micu")) {
            ThreadUtil.execVirtual(() -> {
                fileCacheUtil.writeCacheAndMoov(embyItem);
                fileCacheUtil.cacheNextEpisode(embyItem);
            });
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
            log.warn("视频重定向(缓存):[{}|{}] => {}", mediaSourceId, ua, UrlDecoder.decode(cacheUrl));
            return true;
        }
        return false;
    }

    private String getPtUrl(String cacheUrl) {
        if (!StrUtil.startWithIgnoreCase(cacheUrl, embyConfig.getOriginPt())) {
            return cacheUrl;
        }
        int minute = DateUtil.thisMinute();
        /*if (minute == 0) {
            // 使用原始路径
        } else */
        if (minute % 10 == 0) {
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
        int exTime = 2 * 24 * 60 * 60;
        String realUrl = mediaPath;
        if (StrUtil.startWithIgnoreCase(mediaPath, "http")) {
            // 1. 处理pt/Emby的特殊情况 直接替换为168路径
            if (StrUtil.containsIgnoreCase(mediaPath, "pt/Emby")) {
                for (String strmPath : embyConfig.getStrmPaths()) {
                    realUrl = StrUtil.replaceIgnoreCase(mediaPath, strmPath, embyConfig.getOriginPt());
                }
                realUrl = UrlEncoder.encodeQuery(realUrl);
                // HtmlUtil
                // realut
            } else {
                // 2. head获取处理其他网盘直链远程路径
                Map<String, String> header302 = MapUtil.<String, String>builder()
                        .put("User-Agent", request.getUa()).put("Range", request.getRange()).build();
                realUrl = embyProxy.fetch302Path(mediaPath, header302);
                if (StrUtil.isBlank(realUrl)) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                // 3. 动态计算过期时间
                if (!StrUtil.startWithIgnoreCase(realUrl, embyConfig.getOriginPt())) {
                    exTime = (int) (MapUtil.getLong(UrlQueryUtil.decodeQuery(realUrl, Charset.defaultCharset()),
                            "t") - DateUtil.currentSeconds() - 5 * 60);
                }
            }
            // 4. 统一缓存和重定向逻辑
            String cacheKey = StrUtil.containsAnyIgnoreCase(mediaPath, "pt/Emby")
                    ? CacheUtil.buildVideoCacheKey(mediaSourceId)
                    : CacheUtil.buildVideoCacheKey(mediaSourceId, request.getUa());
            redisClient.set(cacheKey, realUrl, exTime);
            realUrl = getPtUrl(realUrl);
            doRedirect(response, realUrl, exTime, mediaPath);

            return;
        }
        // 5. 需要302的本地路径
        Map<String, String> pathMap = embyConfig.getLocalPathMap();
        String[] localPaths = pathMap.keySet().toArray(new String[0]);
        if (StrUtil.startWithAnyIgnoreCase(mediaPath, localPaths)) {
            // 6. 找到最长匹配前缀的路径映射，避免部分匹配导致的错误替换
            String bestMatchKey = pathMap.keySet().stream()
                    .filter(prefix -> StrUtil.startWithIgnoreCase(mediaPath, prefix))
                    .max(Comparator.comparingInt(String::length))
                    .orElse(null);

            if (bestMatchKey != null) {
                exTime = 30 * 60;
                realUrl = StrUtil.replaceIgnoreCase(mediaPath, bestMatchKey, pathMap.get(bestMatchKey));
                realUrl = UrlEncoder.encodeQuery(realUrl);
                doRedirect(response, realUrl, exTime, mediaPath);
                redisClient.set(CacheUtil.buildVideoCacheKey(mediaSourceId), realUrl, exTime);
                return;
            }
        }
        // 不需要302的本地路径
        originalVideoStream(request, response);
    }

    /**
     * 进行重定向
     *
     * @param response  响应
     * @param realUrl   真实URL
     * @param exTime    前时间
     * @param mediaPath 媒体路径
     */
    private void doRedirect(HttpServletResponse response, String realUrl, int exTime,
                            String mediaPath) {
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", realUrl);
        log.warn("视频重定向({}): [{}] => {}",
                DateUtil.date((DateUtil.currentSeconds() + exTime) * 1000), mediaPath, UrlDecoder.decode(realUrl));
    }

    private void originalVideoStream(EmbyContentCacheReqWrapper request,
                                     HttpServletResponse response) {
        String mediaSourceId = request.getMediaSourceId();
        EmbyItem embyItem = embyProxy.getItemInfo(mediaSourceId);
        if (null == embyItem) {
            response.setStatus(CODE_404);
            return;
        }
        EmbyProxyUtil.Range range = EmbyProxyUtil.parseRangeHeader(request.getRange(), embyItem.getSize());
        if (null == range) {
            response.setHeader("Content-Range", "bytes */" + embyItem.getSize());
            response.setStatus(CODE_416);
            return;
        }
        if (fileCacheUtil.readFile(response, embyItem, range)) {
            return;
        }
        // range = new EmbyProxyUtil.Range(range.start(), embyItem.getSize() - 1, embyItem.getSize());
        String rangeHeader = range.toHeader();
        String ua = embyConfig.getCommonUa();
        Map<String, String> headerMap = MapUtil.<String, String>builder()
                .put("User-Agent", ua).put("range", rangeHeader).build();

        // ThreadUtil.execVirtual(() -> fileCacheUtil.writeFile(request, embyItem, headerMap));
        String mediaPath = CollUtil.getFirst(embyItem.getMediaSources()).getPath();
        Request originalRequest = null;
        if (StrUtil.startWithIgnoreCase(mediaPath, "http")) {
            mediaPath = UrlUtil.normalize(UrlDecoder.decode(mediaPath, Charset.defaultCharset()));
            if (StrUtil.containsAny(mediaPath, "pt/Emby", "bt/Emby")) {
                // mediaPath = EmbyProxyUtil.getPtUrlOnHk(mediaPath);
            } else {
                mediaPath = get302RealUrl(mediaSourceId, ua, mediaPath, headerMap);
            }
            log.warn("原始range:{} total:{}", request.getRange(), embyItem.getSize());
            log.warn("视频拉取(远程):[{}-({})] => {}", mediaSourceId, rangeHeader, mediaPath);
            originalRequest = Request.of(mediaPath).method(Method.GET).header(headerMap).setMaxRedirects(1);
        } else {
            originalRequest = Request.of(embyConfig.getHost() + request.getParamUri())
                    .method(Method.valueOf(request.getMethod()))
                    .body(request.getCachedBody()).header(request.getCachedHeader()).header(headerMap);
            log.warn("视频拉取(本地):[{}-({})] => {}", mediaSourceId, rangeHeader, mediaPath);
        }
        try (Response res = httpClient.send(originalRequest)) {
            response.setStatus(res.getStatus());
            res.headers().forEach((name, values) ->
                    response.setHeader(name, String.join(StrPool.COMMA, values)));
            log.info("返回请求头:{}", res.headers());
            // 使用虚拟线程池（非阻塞模式）
            try (ServletOutputStream out = response.getOutputStream(); InputStream bodyStream = res.bodyStream();
                 ReadableByteChannel inChannel = Channels.newChannel(bodyStream);
                 WritableByteChannel outChannel = Channels.newChannel(out)) {
                // 使用直接缓冲区提升性能（适用于大文件）
                ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 128);
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
            // throw new RuntimeException("Thread interrupted", e);
        }
    }

    private String get302RealUrl(String mediaSourceId, String ua,
                                 String mediaPath, Map<String, String> headerMap) {
        String cacheUrl = redisClient.getStr(CacheUtil.buildVideoCacheKey(mediaSourceId, ua));
        if (StrUtil.isBlank(cacheUrl)) {
            // mediaPath = StrUtil.replace(mediaPath, embyConfig.getAlistPublic(), embyConfig.getAlistInner());
            mediaPath = embyProxy.fetch302Path(mediaPath, headerMap);
            if (StrUtil.isNotBlank(mediaPath)) {
                int exTime = (int) (MapUtil.getLong(UrlQueryUtil.decodeQuery(
                        mediaPath, Charset.defaultCharset()), "t") - DateUtil.currentSeconds() - 5 * 60);
                redisClient.set(CacheUtil.buildVideoCacheKey(mediaSourceId, ua), mediaPath, exTime);
            }
        } else {
            mediaPath = cacheUrl;
        }
        return mediaPath;
    }

    public static void main(String[] args) {
        Console.log(UrlEncoder.encodeQuery("http://192.168.1.205:5244/d/pt/Emby1/动画电影/万王之王 (2025)/万王之王 (2025) - 1080p - REDMT.mkv"));
    }
}