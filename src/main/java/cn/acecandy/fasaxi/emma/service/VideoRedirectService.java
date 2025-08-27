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
import cn.acecandy.fasaxi.emma.utils.ThreadLimitUtil;
import cn.acecandy.fasaxi.emma.utils.VideoUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.date.DateUtil;
import org.dromara.hutool.core.lang.Console;
import org.dromara.hutool.core.lang.mutable.MutablePair;
import org.dromara.hutool.core.map.MapUtil;
import org.dromara.hutool.core.net.url.UrlDecoder;
import org.dromara.hutool.core.net.url.UrlEncoder;
import org.dromara.hutool.core.net.url.UrlQueryUtil;
import org.dromara.hutool.core.net.url.UrlUtil;
import org.dromara.hutool.core.text.StrPool;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.core.text.split.SplitUtil;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_204;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_404;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_416;
import static cn.acecandy.fasaxi.emma.common.enums.CloudStorageType.L_NC2O;
import static cn.acecandy.fasaxi.emma.common.enums.CloudStorageType.R_115;

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
        /*EmbyItem embyItem = embyProxy.getItemInfoByCache(mediaSourceId);
        ThreadUtil.execVirtual(() -> {
            embyProxy.expertTmdbProvider(embyItem);
        });*/
        // if (!StrUtil.containsIgnoreCase(request.getRequestURI(), "download")
        //         && IpUtil.isInnerIp(request.getIp())) {
        //     if (null == embyItem) {
        //         response.setStatus(CODE_404);
        //         return;
        //     }
        //
        //     EmbyProxyUtil.Range range = EmbyProxyUtil.parseRangeHeader(request.getRange(), embyItem.getSize());
        //     if (null == range) {
        //         response.setHeader("Content-Range", "bytes */" + embyItem.getSize());
        //         response.setStatus(CODE_416);
        //         return;
        //     }
        //     if (fileCacheUtil.readFile(response, embyItem, range)) {
        //         return;
        //     }
        // }
        /*if (StrUtil.containsIgnoreCase(embyItem.getPath(), "strm-micu")) {
            ThreadUtil.execVirtual(() -> {
                fileCacheUtil.writeCacheAndMoov(embyItem);
                fileCacheUtil.cacheNextEpisode(embyItem);
            });
        }*/
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
        /*if (minute == 0) {
            // 使用原始路径
        } else */
        /*if (minute % 10 == 0) {
            cacheUrl = StrUtil.replaceIgnoreCase(cacheUrl,
                    embyConfig.getOriginPt(), embyConfig.getTransPt1());
        } else {*/
        /*if (minute % 3 == 0) {
            cacheUrl = StrUtil.replaceIgnoreCase(cacheUrl,
                    embyConfig.getOriginPt(), embyConfig.getTransPt2());
        } else*/
        if (minute % 2 == 0) {
            cacheUrl = StrUtil.replaceIgnoreCase(cacheUrl,
                    embyConfig.getOriginPt(), embyConfig.getTransPt3());
        } else if (minute % 2 == 1) {
            cacheUrl = StrUtil.replaceIgnoreCase(cacheUrl,
                    embyConfig.getOriginPt(), embyConfig.getTransPt4());
        }
        // }
        return cacheUrl;
    }

    private void exec302(EmbyContentCacheReqWrapper request,
                         HttpServletResponse response, String mediaSourceId) {
        EmbyItem itemInfo = embyProxy.getItemInfoByCache(mediaSourceId);
        if (null == itemInfo) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String mediaPath = CollUtil.getFirst(itemInfo.getMediaSources()).getPath();
        mediaPath = StrUtil.replace(UrlDecoder.decode(mediaPath), "(?<!http:|https:)/+", s -> "/");
        int exTime = 24 * 60 * 60;
        String realUrl = mediaPath;
        if (StrUtil.startWithIgnoreCase(mediaPath, "http")) {
            // 1. 处理pt/Emby的特殊情况 直接替换为168路径
            String cloudTypeStr = "micu";
            if (StrUtil.containsIgnoreCase(mediaPath, "pt/Emby")) {
                for (String strmPath : embyConfig.getStrmPaths()) {
                    realUrl = StrUtil.replaceIgnoreCase(mediaPath, strmPath, embyConfig.getOriginPt());
                }
                realUrl = UrlEncoder.encodeQuery(realUrl);
            } else {
                // 2. head获取处理其他网盘直链远程路径
                // 默认替换路径为nc2o路径
                realUrl = UrlEncoder.encodeQuery(StrUtil.replace(mediaPath,
                        "http://192.168.1.249:5244/d", "http://195.128.102.208:5244/p"));
                MutablePair<CloudStorageType, String> cloudTypePair = threadLimitUtil.limitThreadCache(mediaPath);
                CloudStorageType cloudType = cloudTypePair.getLeft();
                if (!cloudType.equals(L_NC2O)) {
                    // String real302Url = cloudUtil.getDownloadUrl(cloudType, request.getUa(), cloudTypePair.getRight(), itemInfo.getSize());
                    String real302Url = cloudUtil.getDownloadUrlOnCopy(cloudType, request.getUa(),
                            request.getDeviceId(), cloudTypePair.getRight(), itemInfo.getSize());
                    if (StrUtil.isBlank(real302Url) && !cloudType.equals(R_115)) {
                        real302Url = cloudUtil.getDownloadUrlOnCopy(R_115, request.getUa(),
                                request.getDeviceId(), cloudTypePair.getRight(), itemInfo.getSize());
                    }
                    if (StrUtil.isNotBlank(real302Url)) {
                        exTime = (int) (MapUtil.getLong(UrlQueryUtil.decodeQuery(real302Url,
                                        Charset.defaultCharset()),
                                "t") - DateUtil.currentSeconds() - 5 * 60);
                        if (StrUtil.containsAny(mediaPath,
                                "/d/new115/emby2/", "/d/new115/embybt/", "/d/new115/other/")) {
                            embyProxy.trans115To123(mediaPath);
                        }
                        threadLimitUtil.setThreadCache(cloudType, request.getDeviceId());
                        cloudTypeStr = cloudType.getValue();
                        realUrl = real302Url;
                    }
                }

                /*
                String path123 = mediaPath;
                String real123 = "";
                String real115 = "";
                if (!) {
                    Map<String, String> header302 = MapUtil.<String, String>builder()
                            .put("User-Agent", request.getUa()).put("Range", request.getRange()).build();
                    real123 = embyProxy.fetch302Path(path123, header302);
                    if (StrUtil.isBlank(real123)) {
                        // 离线到123
                        embyProxy.trans115To123(path115);
                        real115 = embyProxy.fetch302Path(path115, header302);
                        if (StrUtil.isBlank(real115)) {
                            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            return;
                        }
                    }
                    // 3. 动态计算过期时间
                    realUrl = StrUtil.isBlank(real123) ? real115 : real123;
                    exTime = (int) (MapUtil.getLong(UrlQueryUtil.decodeQuery(realUrl, Charset.defaultCharset()),
                            "t") - DateUtil.currentSeconds() - 5 * 60);
                }*/
            }
            // 4. 统一缓存和重定向逻辑
            String cacheKey = "";
            if (StrUtil.containsAnyIgnoreCase(mediaPath, "pt/Emby")) {
                cacheKey = CacheUtil.buildVideoCacheKey(mediaSourceId);
            } else {
                cacheKey = CacheUtil.buildVideoCacheKey(mediaSourceId, request.getDeviceId());
            }
            redisClient.set(cacheKey, cloudTypeStr + "|" + realUrl, exTime);
            realUrl = getPtUrl(realUrl);
            doRedirect(response, realUrl, exTime, mediaPath);

            return;
        }
        // 5. 本地路径通过alist进行302 音乐/里番
        Map<String, String> pathMap = embyConfig.getLocalPathMap();
        String[] localPaths = pathMap.keySet().toArray(new String[0]);
        if (StrUtil.startWithAnyIgnoreCase(mediaPath, localPaths)) {
            // 6. 找到最长匹配前缀的路径映射，避免部分匹配导致的错误替换
            String finalMediaPath = mediaPath;
            String bestMatchKey = pathMap.keySet().stream()
                    .filter(prefix -> StrUtil.startWithIgnoreCase(finalMediaPath, prefix))
                    .max(Comparator.comparingInt(String::length))
                    .orElse(null);

            if (bestMatchKey != null) {
                exTime = 15 * 24 * 60 * 60;
                realUrl = StrUtil.replaceIgnoreCase(mediaPath, bestMatchKey, pathMap.get(bestMatchKey));
                realUrl = UrlEncoder.encodeQuery(realUrl);
                doRedirect(response, realUrl, exTime, mediaPath);
                redisClient.set(CacheUtil.buildVideoCacheKey(mediaSourceId), "local|" + realUrl, exTime);
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

    public static void main(String[] args) {
        Console.log(UrlEncoder.encodeQuery("http://192.168.1.249:5244/d//123/整理/bili/电影解说/927587_木鱼水心/【木鱼微剧场】《加勒比海盗3》/(BV1ox411k7HZ).mp4"));
        Console.log(UrlUtil.normalize("http://192.168.1.249:5244/d//123/整理/bili/电影解说/927587_木鱼水心/【木鱼微剧场】《加勒比海盗3》/(BV1ox411k7HZ).mp4"));
        Console.log(StrUtil.replace("http://192.168.1.249:5244/d//123/整理/bili/电影解说/927587_木鱼水心/【木鱼微剧场】《加勒比海盗3》/(BV1ox411k7HZ).mp4", "//", "/"));
        Console.log(StrUtil.replace("http://192.168.1.249:5244/d//123/整理/bili/电影解说/927587_木鱼水心/【木鱼微剧场】《加勒比海盗3》/(BV1ox411k7HZ).mp4", "(?<!http:|https:)/+", s -> "/"));
        String s = "https://download-cdn.cjjd19.com/123-620/88022c7e/1821373880-0/88022c7eedc34e98b7a1c422d573866c/c-m74?v=5\u0026t=1756143652\u0026s=1756143652c1e20c8f3bedc789620854d253d8535a\u0026r=RAZMEH\u0026bzc=1\u0026bzs=1821373880\u0026bzp=0\u0026bi=2551510957\u0026filename=%2528BV12x411Y7z4%2529.mp4\u0026x-mf-biz-cid=8adede81-475f-43df-92cf-9c0588867fd4-584000\u0026ndcp=1\u0026cache_type=1";
        Console.log(UrlQueryUtil.decodeQuery(s, Charset.defaultCharset()));
        Console.log(MapUtil.getLong(UrlQueryUtil.decodeQuery(s, Charset.defaultCharset()),
                "t"));
    }
}