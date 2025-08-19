package cn.acecandy.fasaxi.emma.utils;

import cn.acecandy.fasaxi.emma.common.enums.CloudStorageType;
import cn.acecandy.fasaxi.emma.sao.dto.Rile;
import org.dromara.hutool.core.map.MapUtil;
import org.dromara.hutool.core.text.StrUtil;

import java.util.List;
import java.util.Map;

/**
 * 云盘 路径缓存工具类
 *
 * @author tangningzhu
 * @since 2025/8/19
 */
public final class CloudPathCacheUtil {
    private CloudPathCacheUtil() {
    }

    private static final Map<String, Rile> CLOUD_CACHE = MapUtil.newSafeConcurrentHashMap();

    /**
     * 缓存key格式
     */
    private static final String KEY_FORMAT = "{}:{}";

    private static String buildCacheKey(CloudStorageType cloudStorage, String path) {
        return StrUtil.format(KEY_FORMAT, cloudStorage, path);
    }

    public static void put(CloudStorageType cloudStorage, String path, Rile file) {
        CLOUD_CACHE.put(buildCacheKey(cloudStorage, path), file);
    }

    public static Rile get(CloudStorageType cloudStorage, String path) {
        if (null == cloudStorage || StrUtil.isBlank(path)) {
            return null;
        }
        return CLOUD_CACHE.get(buildCacheKey(cloudStorage, path));
    }

    public static Rile remove(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }
        return CLOUD_CACHE.remove(key);
    }

    public static Rile remove(CloudStorageType cloudStorage, String path) {
        if (null == cloudStorage || StrUtil.isBlank(path)) {
            return null;
        }
        return remove(buildCacheKey(cloudStorage, path));
    }

    public static void clear() {
        CLOUD_CACHE.clear();
    }

    public static boolean isEmpty() {
        return MapUtil.isEmpty(CLOUD_CACHE);
    }

    public static List<Map.Entry<String, Rile>> getAll(CloudStorageType cloudStorage) {
        return CLOUD_CACHE.entrySet().stream()
                .filter((entry) -> entry.getKey().startsWith(cloudStorage.name() + ":")).toList();
    }

    public static Map.Entry<String, Rile> find(CloudStorageType cloudStorage, String path) {
        return getAll(cloudStorage).stream()
                .filter((entry) ->
                        StrUtil.startWith(buildCacheKey(cloudStorage, path), entry.getKey()))
                .findFirst()
                .orElse(null);
    }
}