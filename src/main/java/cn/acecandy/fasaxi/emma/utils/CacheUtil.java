package cn.acecandy.fasaxi.emma.utils;


import cn.acecandy.fasaxi.emma.common.enums.CloudStorageType;
import cn.acecandy.fasaxi.emma.common.enums.EmbyPicType;
import cn.acecandy.fasaxi.emma.config.EmbyContentCacheReqWrapper;
import cn.hutool.v7.core.cache.impl.FIFOCache;
import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.core.date.DateUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.crypto.SecureUtil;
import cn.hutool.v7.crypto.digest.DigestUtil;
import cn.hutool.v7.json.JSONUtil;

import java.util.List;

import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.DAY_7_MS;

/**
 * emby 工具类
 *
 * @author tangningzhu
 * @since 2024/10/16
 */
public final class CacheUtil extends cn.hutool.v7.core.cache.CacheUtil {
    public static final String THREAD_LIMIT_KEY = "cache:a-thread-limit";

    // 缓存key
    public static final String R_115_TOKEN = "cache:a-115-token";
    public static final String R_115_REFRESH_TOKEN = "cache:a-115-token-refresh";
    public static final String R_123_TOKEN = "cache:a-123-token";
    public static final String R_123_ZONG_TOKEN = "cache:a-123-zong-token";
    public static final String OPENLIST_TOKEN = "cache:openlist-token";
    /**
     * 媒体直链 缓存
     */
    public final static FIFOCache<String, String> MEDIA_CACHE = newFIFOCache(5000, DAY_7_MS);
    private static final String CLOUD_SEARCH_CACHE_KEY = "cache:search:{}|{}|{}";
    private static final String CLOUD_SEARCH_DEVICE_CACHE_KEY = "cache:search:{}|{}-{}|{}";
    private static final String VIDEO_CACHE_KEY = "cache:302video:{}";
    private static final String VIDEO_UA_CACHE_KEY = "cache:302video:{}|{}";
    private static final String PIC_CACHE_KEY = "cache:pic:{}|{}";
    private static final String USER_PERMS_CACHE_KEY = "cache:user-perms:{}";
    private static final String ITEMS_CACHE_KEY = "cache:items:{}";
    private static final String ITEMS_ID_CACHE_KEY = "cache:items-id:{}";
    private static final String ORIGIN_CACHE_KEY = "cache:req:{}|{}";
    private static final String ORIGIN_LATEST_CACHE_KEY = "cache:req-latest:{}|{}";
    private static final String ORIGIN_SHOWNEXT_CACHE_KEY = "cache:req-showNext:{}";
    private static final String THIRD_CACHE_KEY = "cache:third:{}|{}";
    private static final String ORIGIN_CACHE_REFRESH_KEY1 = "cache:req:/emby/Users/{}/Items";
    private static final String ORIGIN_CACHE_REFRESH_KEY2 = "cache:req:/emby/Shows/{}";
    private static final String ORIGIN_CACHE_REFRESH_KEY3 = "cache:req:/Users/{}/Items";
    private static final String ORIGIN_CACHE_REFRESH_KEY1_ALL = "cache:req:/emby/Users/";
    private static final String ORIGIN_CACHE_REFRESH_KEY2_ALL = "cache:req:/Users/";
    private static final String DEVICE_CACHE_FILEID115_KEY = "cache:device-115:{}";
    private CacheUtil() {
    }

    public static String buildThreadLimitKey(CloudStorageType cloudStorageType, String deviceId) {
        return THREAD_LIMIT_KEY + ":" + deviceId + "|" + cloudStorageType.getValue();
    }

    public static String buildThreadLimitKey(String deviceId) {
        return THREAD_LIMIT_KEY + ":" + deviceId;
    }

    /**
     * 设备115文件id缓存key
     *
     * @param deviceId 设备标识符
     * @return {@link String }
     */
    public static String buildDeviceFileId115Key(String deviceId) {
        return StrUtil.format(DEVICE_CACHE_FILEID115_KEY, deviceId + "_" + DateUtil.formatToday());
    }

    /**
     * 云盘搜索缓存key
     *
     * @param param 参数字符串
     * @return {@link String }
     */
    public static String buildCloudSearchKey(CloudStorageType cloudStorage,
                                             String filePath, long size) {
        return StrUtil.format(CLOUD_SEARCH_CACHE_KEY, cloudStorage.getValue(), SecureUtil.md5(filePath), size);
    }

    /**
     * 云盘搜索缓存key
     *
     * @param param 参数字符串
     * @return {@link String }
     */
    public static String buildCloudSearchKey(CloudStorageType cloudStorage, String deviceId,
                                             String filePath, long size) {
        return StrUtil.format(CLOUD_SEARCH_DEVICE_CACHE_KEY, cloudStorage.getValue(),
                deviceId, SecureUtil.md5(filePath), size);
    }

    /**
     * 通过上一个方法名+参数构建 请求第三方的缓存key
     *
     * @param param 参数字符串
     * @return {@link String }
     */
    public static String buildThirdCacheKey(String methodName, String param) {
        return StrUtil.format(THIRD_CACHE_KEY, methodName, param);
    }

    public static String buildOriginCacheKey(EmbyContentCacheReqWrapper req) {
        return StrUtil.format(ORIGIN_CACHE_KEY, req.getRequestURI(),
                DigestUtil.md5Hex16(JSONUtil.toJsonStr(req.getCachedParam())));
    }

    public static String buildOriginLatestCacheKey(String userId, String parentId) {
        return StrUtil.format(ORIGIN_LATEST_CACHE_KEY, userId, parentId);
    }

    public static String buildOriginShowNextCacheKey(String userId) {
        return StrUtil.format(ORIGIN_SHOWNEXT_CACHE_KEY, userId);
    }

    public static List<String> buildOriginRefreshCacheKey(EmbyContentCacheReqWrapper req) {
        List<String> cacheKeys = ListUtil.of();
        if (StrUtil.isNotBlank(req.getUserId())) {
            cacheKeys.add(StrUtil.format(ORIGIN_CACHE_REFRESH_KEY1, req.getUserId()));
            cacheKeys.add(StrUtil.format(ORIGIN_CACHE_REFRESH_KEY3, req.getUserId()));
        }
        return cacheKeys;
    }

    public static List<String> buildOriginRefreshCacheAllKey(EmbyContentCacheReqWrapper req) {
        return ListUtil.of(ORIGIN_CACHE_REFRESH_KEY1_ALL, ORIGIN_CACHE_REFRESH_KEY2_ALL);
    }

    public static String buildVideoCacheKey(String mediaSourceId) {
        return StrUtil.format(VIDEO_CACHE_KEY, mediaSourceId);
    }

    public static String buildVideoCacheKey(String mediaSourceId, String deviceId) {
        return StrUtil.format(VIDEO_UA_CACHE_KEY, mediaSourceId, deviceId);
    }

    public static List<String> buildVideoCacheKeyList(String mediaSourceId,
                                                      String deviceId) {
        return ListUtil.of(buildVideoCacheKey(mediaSourceId, deviceId),
                buildVideoCacheKey(mediaSourceId));
    }

    public static String buildPicCacheKey(String itemId, EmbyPicType picType) {
        return StrUtil.format(PIC_CACHE_KEY, itemId, picType.getValue());
    }

    public static String buildUserPermsCacheKey(String userId) {
        return StrUtil.format(USER_PERMS_CACHE_KEY, userId);
    }

    public static String buildItemsCacheKey(String parentId) {
        return StrUtil.format(ITEMS_CACHE_KEY, parentId);
    }

    public static String buildItemsIdCacheKey(String parentId) {
        return StrUtil.format(ITEMS_ID_CACHE_KEY, parentId);
    }

    private static String mediaCacheKey(String ua, String mediaSourceId) {
        return StrUtil.format(VIDEO_CACHE_KEY, mediaSourceId, ua);
    }

    public static void setMediaKey(String ua, String mediaSourceId, String value) {
        MEDIA_CACHE.put(mediaCacheKey(ua, mediaSourceId), value);
    }

    /**
     * 获取对应缓存
     * <p>
     * 先查询ua为空的缓存，有就返回（主要是非115不需要ua） 没有再查询ua不为空的缓存
     *
     * @param ua            ua
     * @param mediaSourceId 媒体源id
     * @return {@link String }
     */
    public static String getMediaKey(String ua, String mediaSourceId) {
        String cache = MEDIA_CACHE.get(mediaCacheKey("", mediaSourceId));
        if (StrUtil.isBlank(cache)) {
            return MEDIA_CACHE.get(mediaCacheKey(ua, mediaSourceId));
        }
        return cache;
    }

}