package cn.acecandy.fasaxi.emma.service;

import cn.acecandy.fasaxi.emma.common.enums.CloudStorageType;
import cn.acecandy.fasaxi.emma.config.EmbyConfig;
import cn.acecandy.fasaxi.emma.config.EmbyContentCacheReqWrapper;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.out.EmbyItem;
import cn.acecandy.fasaxi.emma.sao.proxy.EmbyProxy;
import cn.acecandy.fasaxi.emma.utils.CacheUtil;
import cn.acecandy.fasaxi.emma.utils.CloudUtil;
import cn.acecandy.fasaxi.emma.utils.EmbyProxyUtil;
import cn.acecandy.fasaxi.emma.utils.FileCacheUtil;
import cn.acecandy.fasaxi.emma.utils.LockUtil;
import cn.acecandy.fasaxi.emma.utils.PathUtil;
import cn.acecandy.fasaxi.emma.utils.ThreadLimitUtil;
import cn.acecandy.fasaxi.emma.utils.ThreadUtil;
import cn.acecandy.fasaxi.emma.utils.VideoUtil;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.date.DateUtil;
import cn.hutool.v7.core.lang.mutable.MutablePair;
import cn.hutool.v7.core.map.MapUtil;
import cn.hutool.v7.core.net.url.UrlDecoder;
import cn.hutool.v7.core.net.url.UrlEncoder;
import cn.hutool.v7.core.net.url.UrlQueryUtil;
import cn.hutool.v7.core.net.url.UrlUtil;
import cn.hutool.v7.core.text.StrPool;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.core.text.split.SplitUtil;
import cn.hutool.v7.http.client.Request;
import cn.hutool.v7.http.client.Response;
import cn.hutool.v7.http.client.engine.ClientEngine;
import cn.hutool.v7.http.meta.Method;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_204;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_404;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_416;
import static cn.acecandy.fasaxi.emma.common.enums.CloudStorageType.L_NC2O;
import static cn.acecandy.fasaxi.emma.common.enums.CloudStorageType.R_115;
import static cn.acecandy.fasaxi.emma.common.enums.CloudStorageType.R_123_ZONG;

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
    private ThreadLimitUtil threadLimitUtil;

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

    @Resource
    private CloudUtil cloudUtil;

    @SneakyThrows
    public void processVideo(EmbyContentCacheReqWrapper request, HttpServletResponse response) {
        String mediaSourceId = request.getMediaSourceId();
        if (StrUtil.isBlank(mediaSourceId)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String deviceId = request.getDeviceId();
        if (getByCache(request, response, mediaSourceId, deviceId)) {
            return;
        }

        // 获取或创建对应的锁
        Lock lock = LockUtil.lockVideo(mediaSourceId);
        if (LockUtil.isLock1s(lock)) {
            response.setStatus(CODE_204);
            return;
        }
        try {
            if (getByCache(request, response, mediaSourceId, deviceId)) {
                return;
            }
            exec302(request, response, mediaSourceId);
        } finally {
            LockUtil.unlockVideo(lock, mediaSourceId);
        }
    }

    private boolean getByCache(EmbyContentCacheReqWrapper request,
                               HttpServletResponse response,
                               String mediaSourceId, String deviceId) {
        String cacheUrl = redisClient.getStrFindOne(CacheUtil.buildVideoCacheKeyList(mediaSourceId, deviceId));
        if (StrUtil.isNotBlank(cacheUrl)) {
            List<String> urlSeg = SplitUtil.splitTrim(cacheUrl, "|");
            CloudStorageType cloudStorageType = CloudStorageType.of(CollUtil.getFirst(urlSeg));
            cacheUrl = CollUtil.getLast(urlSeg);
            cacheUrl = getPtUrl(cacheUrl);
            threadLimitUtil.setThreadCache(cloudStorageType, deviceId);

            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", cacheUrl);
            response.setHeader("Referrer-Policy", "no-referrer");
            log.warn("视频重定向(缓存):[{}|{}] => {}", mediaSourceId, deviceId, UrlDecoder.decode(cacheUrl));
            return true;
        }
        return false;
    }

    private String getPtUrl(String cacheUrl) {
        if (!StrUtil.startWithIgnoreCase(cacheUrl, embyConfig.getOriginPt())) {
            return cacheUrl;
        }
        int minute = DateUtil.thisMinute();
        if (minute % 2 == 0) {
            cacheUrl = StrUtil.replaceIgnoreCase(cacheUrl,
                    embyConfig.getOriginPt(), embyConfig.getTransPt3());
        } else if (minute % 2 == 1) {
            cacheUrl = StrUtil.replaceIgnoreCase(cacheUrl,
                    embyConfig.getOriginPt(), embyConfig.getTransPt4());
        }
        return cacheUrl;
    }

    private void exec302(EmbyContentCacheReqWrapper request,
                         HttpServletResponse response, String mediaSourceId) {
        MediaInfo mediaInfo = getMediaInfo(mediaSourceId);
        if (mediaInfo == null) {
            response.setStatus(CODE_404);
            return;
        }
        // 3. 根据路径类型分发处理
        RedirectResult result = processMediaPath(mediaInfo, request);
        // 4. 执行重定向和缓存
        executeRedirect(response, result, mediaSourceId, request.getDeviceId());
    }

    private RedirectResult processMediaPath(MediaInfo mediaInfo, EmbyContentCacheReqWrapper request) {
        String mediaPath = mediaInfo.path;

        if (StrUtil.startWithIgnoreCase(mediaPath, "http")) {
            return processHttpPath(mediaInfo, request);
        }

        // 本地视频转alist
        if (embyConfig.isLocalPath(mediaPath)) {
            return processLocalPath(mediaInfo);
        }

        // 默认处理：本地路径直接返回
        return new RedirectResult(mediaPath, "local", getDefaultExpireTime(), mediaInfo.path);
    }

    private RedirectResult processHttpPath(MediaInfo mediaInfo, EmbyContentCacheReqWrapper request) {
        String mediaPath = mediaInfo.path;

        // 处理pt/Emby特殊情况
        if (StrUtil.containsIgnoreCase(mediaPath, "pt/Emby")) {
            // 替换strm路径为originPt路径
            mediaPath = PathUtil.replaceAfterUrlPath(mediaPath, "/d/pt", embyConfig.getOriginPt());
            mediaPath = UrlEncoder.encodeQuery(mediaPath);
            return new RedirectResult(mediaPath, "micu", 24 * 60 * 60, mediaInfo.path);
        }

        // 处理其他网盘路径
        return processCloudStoragePath(mediaInfo, request);
    }

    private RedirectResult processCloudStoragePath(MediaInfo mediaInfo,
                                                   EmbyContentCacheReqWrapper request) {
        String mediaPath = mediaInfo.path;

        // 默认替换为nc2o路径
        String realUrl = UrlEncoder.encodeQuery(StrUtil.replace(mediaPath,
                "http://192.168.1.249:5244/d", "http://195.128.102.208:5244/p"));

        MutablePair<CloudStorageType, String> cloudTypePair = threadLimitUtil.limitThreadCache(mediaPath);
        CloudStorageType cloudType = cloudTypePair.getLeft();

        if (cloudType.equals(L_NC2O)) {
            return new RedirectResult(realUrl, "nc2o", 24 * 60 * 60, mediaInfo.path);
        }

        // 获取下载URL 如果是115先获取复制的 没有的话复制并查询获取；如果是123的直接获取
        String real302Url = cloudUtil.getDownloadUrlOnCopyByOpenlist(cloudType, request.getUa(),
                request.getDeviceId(), cloudTypePair.getRight());

        // 如果获取失败且不是115网盘，尝试使用115网盘
        if (StrUtil.isBlank(real302Url) && cloudType.equals(R_123_ZONG)) {
            real302Url = cloudUtil.getDownloadUrlOnCopyByOpenlist(R_115, request.getUa(),
                    request.getDeviceId(), cloudTypePair.getRight());
        }

        if (StrUtil.isNotBlank(real302Url)) {
            int exTime = calculateExpireTime(real302Url);

            // 特殊路径处理：115转123
            ThreadUtil.execute(() -> {
                if (StrUtil.contains(mediaPath, "/d/new115/")) {
                    embyProxy.trans115To123(mediaPath);
                }
            });
            threadLimitUtil.setThreadCache(cloudType, request.getDeviceId());
            return new RedirectResult(real302Url, cloudType.getValue(), exTime, mediaInfo.path);
        }

        return new RedirectResult(realUrl, "nc2o", 2 * 60 * 60, mediaInfo.path);
    }

    private RedirectResult processLocalPath(MediaInfo mediaInfo) {
        String mediaPath = mediaInfo.path();
        Map<String, String> pathMap = embyConfig.getLocalPathMap();

        // 找到最长匹配前缀的路径映射
        String bestMatchKey = pathMap.keySet().stream()
                .filter(prefix -> StrUtil.startWithIgnoreCase(mediaPath, prefix))
                .max(Comparator.comparingInt(String::length))
                .orElse(null);

        if (bestMatchKey != null) {
            String realUrl = StrUtil.replaceIgnoreCase(mediaPath, bestMatchKey, pathMap.get(bestMatchKey));
            realUrl = UrlEncoder.encodeQuery(realUrl);
            return new RedirectResult(realUrl, "local", 15 * 24 * 60 * 60, mediaInfo.path);
        }

        return new RedirectResult(mediaPath, "local", getDefaultExpireTime(), mediaInfo.path);
    }

    private void executeRedirect(HttpServletResponse response, RedirectResult result,
                                 String mediaSourceId, String deviceId) {
        if (result == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        // 构建缓存key
        String cacheKey = buildCacheKey(result.storageType(), mediaSourceId, deviceId, result.url());

        // 设置Redis缓存
        String cacheValue = result.storageType() + "|" + result.url();
        redisClient.set(cacheKey, cacheValue, result.expireTime());

        // 处理pt URL
        String redirectUrl = getPtUrl(result.url());

        // 执行重定向
        doRedirect(response, redirectUrl, result.expireTime(), result.originalPath());
    }

    private String buildCacheKey(String storageType, String mediaSourceId,
                                 String deviceId, String url) {
        if (StrUtil.equalsAnyIgnoreCase(storageType, "local", "micu")) {
            return CacheUtil.buildVideoCacheKey(mediaSourceId);
        } else {
            return CacheUtil.buildVideoCacheKey(mediaSourceId, deviceId);
        }
    }

    private int calculateExpireTime(String real302Url) {
        try {
            long expireTime = MapUtil.getLong(UrlQueryUtil.decodeQuery(real302Url,
                    Charset.defaultCharset()), "t") - DateUtil.currentSeconds() - 5 * 60;
            // 最少保留60秒
            return Math.max((int) expireTime, 10 * 60);
        } catch (Exception e) {
            // 默认2小时
            return 2 * 60 * 60;
        }
    }

    private int getDefaultExpireTime() {
        return 24 * 60 * 60;
    }

    /**
     * 通过id获取媒体路径和大小
     *
     * @param mediaSourceId 媒体源id
     * @return {@link MediaInfo }
     */
    private MediaInfo getMediaInfo(String mediaSourceId) {
        EmbyItem itemInfo = embyProxy.getItemInfoByCache(mediaSourceId);
        if (itemInfo == null || CollUtil.isEmpty(itemInfo.getMediaSources())) {
            return null;
        }

        String mediaPath = CollUtil.getFirst(itemInfo.getMediaSources()).getPath();
        mediaPath = StrUtil.replace(UrlDecoder.decode(mediaPath), "(?<!http:|https:)/+", s -> "/");

        return new MediaInfo(mediaPath, itemInfo.getSize());
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
        response.setHeader("Referrer-Policy", "no-referrer");
        log.warn("视频重定向({}): [{}] => {}",
                DateUtil.date((DateUtil.currentSeconds() + exTime) * 1000), mediaPath, UrlDecoder.decode(realUrl));
    }

    private void originalVideoStream(EmbyContentCacheReqWrapper request,
                                     HttpServletResponse response) {
        String mediaSourceId = request.getMediaSourceId();
        EmbyItem embyItem = embyProxy.getItemInfoByCache(mediaSourceId);
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
            mediaPath = UrlUtil.normalize(UrlDecoder.decode(mediaPath));
            if (StrUtil.containsAny(mediaPath, "pt/Emby", "bt/Emby")) {
                // mediaPath = EmbyProxyUtil.getPtUrlOnHk(mediaPath);
            } else {
                mediaPath = get302RealUrl(mediaSourceId, request.getDeviceId(), mediaPath, headerMap);
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

    private String get302RealUrl(String mediaSourceId, String deviceId,
                                 String mediaPath, Map<String, String> headerMap) {
        String cacheUrl = redisClient.getStr(CacheUtil.buildVideoCacheKey(mediaSourceId, deviceId));
        if (StrUtil.isBlank(cacheUrl)) {
            // mediaPath = StrUtil.replace(mediaPath, embyConfig.getAlistPublic(), embyConfig.getAlistInner());
            mediaPath = embyProxy.fetch302Path(mediaPath, headerMap);
            if (StrUtil.isNotBlank(mediaPath)) {
                int exTime = (int) (MapUtil.getLong(UrlQueryUtil.decodeQuery(
                        mediaPath, Charset.defaultCharset()), "t") - DateUtil.currentSeconds() - 5 * 60);
                redisClient.set(CacheUtil.buildVideoCacheKey(mediaSourceId, deviceId), mediaPath, exTime);
            }
        } else {
            mediaPath = cacheUrl;
        }
        return mediaPath;
    }

    /**
     * 临时存放mediaPath和size的实体
     *
     * @author AceCandy
     * @since 2025/09/28
     */
    record MediaInfo(String path, long size) {

    }

    record RedirectResult(String url, String storageType, int expireTime, String originalPath) {
    }
}