package cn.acecandy.fasaxi.emma.utils;


import cn.acecandy.fasaxi.emma.common.enums.EmbyPicType;
import org.dromara.hutool.core.cache.impl.FIFOCache;
import org.dromara.hutool.core.collection.ListUtil;
import org.dromara.hutool.core.text.StrUtil;

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


    public static String buildVideoCacheKey(String mediaSourceId) {
        return StrUtil.format(VIDEO_CACHE_KEY, mediaSourceId);
    }

    public static String buildVideoCacheKey(String mediaSourceId, String ua) {
        return StrUtil.format(VIDEO_UA_CACHE_KEY, mediaSourceId, ua);
    }

    public static List<String> buildVideoCacheKeyList(String mediaSourceId, String ua) {
        return ListUtil.of(buildVideoCacheKey(mediaSourceId,ua), buildVideoCacheKey(mediaSourceId));
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