package cn.acecandy.fasaxi.emma.service;

import cn.acecandy.fasaxi.emma.common.enums.CloudStorageType;
import cn.acecandy.fasaxi.emma.common.enums.StrmPathPrefix;
import cn.acecandy.fasaxi.emma.config.EmbyConfig;
import cn.acecandy.fasaxi.emma.config.EmbyContentCacheReqWrapper;
import cn.acecandy.fasaxi.emma.config.OpConfig;
import cn.acecandy.fasaxi.emma.dao.embyboss.entity.VideoPathRelation;
import cn.acecandy.fasaxi.emma.dao.embyboss.service.VideoPathRelationDao;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.client.RedisLockClient;
import cn.acecandy.fasaxi.emma.sao.out.EmbyItem;
import cn.acecandy.fasaxi.emma.sao.proxy.EmbyProxy;
import cn.acecandy.fasaxi.emma.utils.CacheUtil;
import cn.acecandy.fasaxi.emma.utils.CloudUtil;
import cn.acecandy.fasaxi.emma.utils.PathUtil;
import cn.acecandy.fasaxi.emma.utils.ThreadLimitUtil;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.date.DateUtil;
import cn.hutool.v7.core.lang.mutable.MutablePair;
import cn.hutool.v7.core.map.MapUtil;
import cn.hutool.v7.core.net.url.UrlDecoder;
import cn.hutool.v7.core.net.url.UrlEncoder;
import cn.hutool.v7.core.net.url.UrlQueryUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.core.text.split.SplitUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_204;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_404;
import static cn.acecandy.fasaxi.emma.common.enums.CloudStorageType.L_MICU;
import static cn.acecandy.fasaxi.emma.common.enums.CloudStorageType.L_NC2O;
import static cn.acecandy.fasaxi.emma.sao.client.RedisLockClient.buildVideoLock;

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
    private ThreadLimitUtil threadLimitUtil;

    @Resource
    private RedisClient redisClient;

    @Resource
    private EmbyConfig embyConfig;

    @Resource
    private EmbyProxy embyProxy;

    @Resource
    private CloudUtil cloudUtil;

    @Resource
    private RedisLockClient redisLockClient;

    @Resource
    private VideoPathRelationDao videoPathRelationDao;

    @Resource
    private OpConfig opConfig;

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
        String lockKey = buildVideoLock(mediaSourceId);
        if (!redisLockClient.lock(lockKey)) {
            response.setStatus(CODE_204);
            return;
        }
        try {
            if (getByCache(request, response, mediaSourceId, deviceId)) {
                return;
            }
            exec302(request, response, mediaSourceId);
        } finally {
            redisLockClient.unlock(lockKey);
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
            cacheUrl = getPtUrl(cacheUrl, cloudStorageType.getValue());
            threadLimitUtil.setThreadCache(cloudStorageType, deviceId);

            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", cacheUrl);
            response.setHeader("Referrer-Policy", "no-referrer");
            log.warn("视频重定向(缓存):[{}|{}] => {}", mediaSourceId, deviceId, UrlDecoder.decode(cacheUrl));
            return true;
        }
        return false;
    }

    private String getPtUrl(String cacheUrl, String cloudStorageType) {
        if (!StrUtil.startWithIgnoreCase(cacheUrl, embyConfig.getOriginPt())
                && !StrUtil.equals(cloudStorageType, L_MICU.getValue())) {
            return cacheUrl;
        }
        cacheUrl = StrUtil.replaceIgnoreCase(cacheUrl,
                opConfig.getHost(), embyConfig.getOriginPt());
        cacheUrl = StrUtil.removeAll(cacheUrl, "/d/pt");

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
        RedirectResult result = null;
        VideoPathRelation vpr = videoPathRelationDao.findById(Integer.parseInt(mediaSourceId));
        if (vpr != null) {
            result = processRelationPath(vpr, request, mediaSourceId);
        } else {
            // 3. 根据路径类型分发处理
            result = processMediaPath(mediaInfo, request);

            if (result == null) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
            cloudUtil.cacheOpenList302Url(result, mediaSourceId, request.getDeviceId());
        }

        // 异步处理下一集
        embyProxy.nextHandle(request.getUserId(), request.getUa(), request.getDeviceId(), mediaInfo.itemInfo);

        // 4. 执行重定向和缓存
        String redirectUrl = getPtUrl(result.url(), result.storageType());
        // 执行重定向
        doRedirect(response, redirectUrl, result.expireTime(), result.originalPath());
    }

    private RedirectResult processRelationPath(VideoPathRelation vpr,
                                               EmbyContentCacheReqWrapper request,
                                               String mediaSourceId) {
        String storageType = vpr.getStrmType();
        if (StrUtil.equals(storageType, StrmPathPrefix.PRE_LOCAL.getType())) {
            return processLocalRelationPath(vpr, request);
        }

        RelationPath relationPath = resolveCloudRelationPath(vpr, storageType);
        String url302 = cloudUtil.reqAndCacheOpenList302Url(CloudStorageType.of(relationPath.storageType()),
                relationPath.redirectPath(), request.getUa(), mediaSourceId, request.getDeviceId());
        return new RedirectResult(url302, relationPath.storageType(),
                CacheUtil.getVideoDefaultExpireTime(), relationPath.originalPath());
    }

    private RedirectResult processLocalRelationPath(VideoPathRelation vpr,
                                                    EmbyContentCacheReqWrapper request) {
        String originalPath = vpr.getRealPath();
        String normalizedPath = embyConfig.normalizeStrmPath(originalPath);

        // 兼容历史 relation 里已写成 local 的镜像路径，播放时按规范化后的路径重新分流。
        if (StrUtil.startWithIgnoreCase(normalizedPath, "http")) {
            return processHttpPath(originalPath, normalizedPath, request);
        }

        // 本地关系表命中后，无论是否配置了路径映射，都要返回一个可用结果，避免空指针。
        return processLocalPath(originalPath);
    }

    private RelationPath resolveCloudRelationPath(VideoPathRelation vpr, String storageType) {
        String originalPath = opConfig.getHost() + vpr.getPathPrefix() + vpr.getPurePath();
        String redirectPath = originalPath;

        String url123 = vpr.getPath123();
        if (StrUtil.isNotBlank(url123)) {
            return new RelationPath(originalPath, url123, StrmPathPrefix.PRE_ZONG123.getType());
        }
        if (StrUtil.isNotBlank(vpr.getPath115())) {
            redirectPath = StrUtil.replace(vpr.getPath115(), "/new115/", "/new115-ck/");
            return new RelationPath(originalPath, redirectPath, StrmPathPrefix.PRE_115.getType());
        }
        return new RelationPath(originalPath, redirectPath, storageType);
    }

    private RedirectResult processMediaPath(MediaInfo mediaInfo, EmbyContentCacheReqWrapper request) {
        return processMediaPath(mediaInfo.path, request);
    }

    private RedirectResult processMediaPath(String originalPath, EmbyContentCacheReqWrapper request) {
        String mediaPath = embyConfig.normalizeStrmPath(originalPath);

        if (StrUtil.startWithIgnoreCase(mediaPath, "http")) {
            return processHttpPath(originalPath, mediaPath, request);
        }

        // 本地视频转alist
        if (embyConfig.isLocalPath(originalPath)) {
            return processLocalPath(originalPath, 15 * 24 * 60 * 60,
                    CacheUtil.getVideoDefaultExpireTime(), originalPath);
        }

        // 默认处理：本地路径直接返回
        return new RedirectResult(originalPath, "local", CacheUtil.getVideoDefaultExpireTime(), originalPath);
    }

    private RedirectResult processHttpPath(String originalPath, String mediaPath,
                                           EmbyContentCacheReqWrapper request) {
        // 处理pt/Emby特殊情况
        if (StrUtil.containsIgnoreCase(mediaPath, "pt/Emby")) {
            // 替换strm路径为originPt路径
            mediaPath = PathUtil.replaceAfterUrlPath(mediaPath, "/d/pt", embyConfig.getOriginPt());
            mediaPath = UrlEncoder.encodeQuery(mediaPath);
            return new RedirectResult(mediaPath, "micu", 24 * 60 * 60, originalPath);
        }

        // 处理其他网盘路径
        return processCloudStoragePath(originalPath, mediaPath, request);
    }

    private RedirectResult processCloudStoragePath(String originalPath, String mediaPath,
                                                   EmbyContentCacheReqWrapper request) {
        // 默认替换为nc2o路径
        String realUrl = buildNc2oFallbackUrl(mediaPath);

        MutablePair<CloudStorageType, String> cloudTypePair = threadLimitUtil.limitThreadCache(mediaPath);
        CloudStorageType cloudType = cloudTypePair.getLeft();

        if (cloudType.equals(L_NC2O)) {
            return new RedirectResult(realUrl, "nc2o", 24 * 60 * 60, originalPath);
        }

        // 获取下载URL 如果是115先获取复制的 没有的话复制并查询获取；如果是123的直接获取
        String real302Url = cloudUtil.getDownloadUrlOnCopyByOpenlist(cloudType, request.getUa(),
                request.getDeviceId(), cloudTypePair.getRight());

        if (StrUtil.isNotBlank(real302Url)) {
            int exTime = calculateExpireTime(real302Url);
            threadLimitUtil.setThreadCache(cloudType, request.getDeviceId());
            return new RedirectResult(real302Url, cloudType.getValue(), exTime, originalPath);
        }

        return new RedirectResult(realUrl, "nc2o", 2 * 60 * 60, originalPath);
    }

    private String buildNc2oFallbackUrl(String mediaPath) {
        if (StrUtil.isBlank(mediaPath)) {
            return mediaPath;
        }
        String fallbackBase = resolveNc2oFallbackBase();
        if (StrUtil.isBlank(fallbackBase)) {
            return UrlEncoder.encodeQuery(mediaPath);
        }
        return UrlEncoder.encodeQuery(PathUtil.replaceAfterUrlPath(mediaPath, "/d", fallbackBase));
    }

    private String resolveNc2oFallbackBase() {
        String[] candidates = {embyConfig.getTransPt3(), embyConfig.getTransPt4(),
                embyConfig.getTransPt1(), embyConfig.getTransPt2(), embyConfig.getOriginPt()};
        for (String candidate : candidates) {
            if (StrUtil.isBlank(candidate)) {
                continue;
            }
            String normalized = StrUtil.removeSuffix(candidate, "/");
            if (StrUtil.endWithIgnoreCase(normalized, "/pt")) {
                return StrUtil.removeSuffix(normalized, "/pt");
            }
            return normalized;
        }
        return "";
    }

    private RedirectResult processLocalPath(MediaInfo mediaInfo) {
        return processLocalPath(mediaInfo.path(), 15 * 24 * 60 * 60,
                CacheUtil.getVideoDefaultExpireTime(), mediaInfo.path);
    }

    private RedirectResult processLocalPath(String mediaPath) {
        return processLocalPath(mediaPath, 30 * 60, 2 * 60, mediaPath);
    }

    private RedirectResult processLocalPath(String mediaPath, int matchedExpireTime,
                                            int fallbackExpireTime, String originalPath) {
        Map<String, String> pathMap = embyConfig.getLocalPathMap();

        // 找到最长匹配前缀的路径映射
        String bestMatchKey = pathMap.keySet().stream()
                .filter(prefix -> StrUtil.startWithIgnoreCase(mediaPath, prefix))
                .max(Comparator.comparingInt(String::length))
                .orElse(null);

        if (bestMatchKey != null) {
            String realUrl = StrUtil.replaceIgnoreCase(mediaPath, bestMatchKey, pathMap.get(bestMatchKey));
            realUrl = UrlEncoder.encodeQuery(realUrl);
            return new RedirectResult(realUrl, "local", matchedExpireTime, originalPath);
        }

        return new RedirectResult(mediaPath, "local", fallbackExpireTime, originalPath);
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

    private MediaInfo getMediaInfo(String mediaSourceId) {
        List<EmbyItem> itemInfos = embyProxy.getItemInfoByCache(mediaSourceId);
        if (CollUtil.isEmpty(itemInfos)) {
            return null;
        }
        EmbyItem itemInfo = CollUtil.getFirst(itemInfos);

        String mediaPath = CollUtil.getFirst(itemInfo.getMediaSources()).getPath();
        mediaPath = StrUtil.replace(UrlDecoder.decode(mediaPath), "(?<!http:|https:)/+", s -> "/");

        return new MediaInfo(itemInfo, mediaPath, itemInfo.getSize());
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

    /**
     * 临时存放mediaPath和size的实体
     *
     * @author AceCandy
     * @since 2025/09/28
     */
    record MediaInfo(EmbyItem itemInfo, String path, long size) {

    }

    public record RedirectResult(String url, String storageType, int expireTime, String originalPath) {
    }

    private record RelationPath(String originalPath, String redirectPath, String storageType) {
    }
}
