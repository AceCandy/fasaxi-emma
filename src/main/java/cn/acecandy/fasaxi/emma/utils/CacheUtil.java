package cn.acecandy.fasaxi.emma.utils;


import cn.acecandy.fasaxi.emma.common.enums.EmbyPicType;
import cn.acecandy.fasaxi.emma.config.EmbyContentCacheReqWrapper;
import org.dromara.hutool.core.cache.impl.FIFOCache;
import org.dromara.hutool.core.collection.ListUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.crypto.digest.DigestUtil;
import org.dromara.hutool.json.JSONUtil;

import java.util.List;

import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.DAY_7_MS;

/**
 * emby 工具类
 *
 * @author tangningzhu
 * @since 2024/10/16
 */
public final class CacheUtil extends org.dromara.hutool.core.cache.CacheUtil {
    private CacheUtil() {
    }

    // 缓存key

    private static final String VIDEO_CACHE_KEY = "cache:video:{}";
    private static final String VIDEO_UA_CACHE_KEY = "cache:video:{}|{}";
    private static final String PIC_CACHE_KEY = "cache:pic:{}|{}";
    private static final String ORIGIN_CACHE_KEY = "cache:req:{}|{}";

    private static final String THIRD_CACHE_KEY = "cache:third:{}|{}";

    private static final String ORIGIN_CACHE_REFRESH_KEY1 = "cache:req:/emby/Users/{}/Items";
    private static final String ORIGIN_CACHE_REFRESH_KEY2 = "cache:req:/emby/Shows/{}";
    private static final String ORIGIN_CACHE_REFRESH_KEY3 = "cache:req:/Users/{}/Items";

    private static final String ORIGIN_CACHE_REFRESH_KEY1_ALL = "cache:req:/emby/Users/";
    private static final String ORIGIN_CACHE_REFRESH_KEY2_ALL = "cache:req:/Users/";


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

    public static String buildVideoCacheKey(String mediaSourceId, String ua) {
        return StrUtil.format(VIDEO_UA_CACHE_KEY, mediaSourceId, ua);
    }

    public static List<String> buildVideoCacheKeyList(String mediaSourceId, String ua) {
        return ListUtil.of(buildVideoCacheKey(mediaSourceId, ua), buildVideoCacheKey(mediaSourceId));
    }

    public static String buildPicCacheKey(String itemId, EmbyPicType picType) {
        return StrUtil.format(PIC_CACHE_KEY, itemId, picType.getValue());
    }


    /**
     * 媒体直链 缓存
     */
    public final static FIFOCache<String, String> MEDIA_CACHE = newFIFOCache(5000, DAY_7_MS);

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