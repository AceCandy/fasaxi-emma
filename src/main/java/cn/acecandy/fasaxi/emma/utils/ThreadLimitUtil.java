package cn.acecandy.fasaxi.emma.utils;

import cn.acecandy.fasaxi.emma.common.enums.CloudStorageType;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.lang.mutable.MutablePair;
import org.dromara.hutool.core.map.MapUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.core.text.split.SplitUtil;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

import static cn.acecandy.fasaxi.emma.common.enums.CloudStorageType.L_NC2O;
import static cn.acecandy.fasaxi.emma.common.enums.CloudStorageType.R_115;
import static cn.acecandy.fasaxi.emma.common.enums.CloudStorageType.R_123;
import static cn.acecandy.fasaxi.emma.common.enums.CloudStorageType.R_123_ZONG;
import static cn.acecandy.fasaxi.emma.utils.CacheUtil.THREAD_LIMIT_KEY;

/**
 * emby 工具类
 *
 * @author tangningzhu
 * @since 2024/10/16
 */
@Slf4j
@Component
public class ThreadLimitUtil {
    @Resource
    private RedisClient redisClient;
    /**
     * EXP时间 2hour 防止重启导致的key丢失
     */
    private final static Integer EXP_TIME = 60 * 60 * 2;

    /**
     * 设置线程缓存
     *
     * @param cloudStorageType 云盘类型 115/123
     * @param deviceId         设备ID
     */
    public synchronized void setThreadCache(CloudStorageType cloudStorageType, String deviceId) {
        if (cloudStorageType == null) {
            return;
        }
        redisClient.set(CacheUtil.buildThreadLimitKey(cloudStorageType, deviceId), 1, EXP_TIME);
    }

    /**
     * 移除线程缓存
     *
     * @param deviceId 设备ID
     */
    public void removeThreadCache(String deviceId) {
        redisClient.delByPrefix(CacheUtil.buildThreadLimitKey(deviceId));
    }

    /**
     * 线程超限检测
     *
     * @return 返回输出类型 CloudStorageType
     */
    public synchronized MutablePair<CloudStorageType, String> limitThreadCache(String mediaPath) {
        if (StrUtil.containsAny(mediaPath, "/d/new115/")) {
            return handleNew115Path(mediaPath);
        }
        if (StrUtil.contains(mediaPath, "/d/123/")) {
            return handle123Path(mediaPath);
        }
        if (StrUtil.contains(mediaPath, "/d/zong123/")) {
            return handleZong123Path(mediaPath);
        }
        return MutablePair.of(L_NC2O, "");
    }

    private MutablePair<CloudStorageType, String> handleNew115Path(String mediaPath) {
        Map<CloudStorageType, Integer> typeCountMap = getCloudStorageTypeCount();

        if (MapUtil.getInt(typeCountMap, R_115, 0) <= 3) {
            return MutablePair.of(R_115, StrUtil.removePrefix(mediaPath, "http://192.168.1.249:5244/d/new115"));
        }
        if (MapUtil.getInt(typeCountMap, R_123_ZONG, 0) <= 4) {
            return MutablePair.of(R_123_ZONG,
                    StrUtil.removePrefix(mediaPath, "http://192.168.1.249:5244/d/new115"));
        }
        return MutablePair.of(L_NC2O, "");
    }

    private MutablePair<CloudStorageType, String> handle123Path(String mediaPath) {
        Map<CloudStorageType, Integer> typeCountMap = getCloudStorageTypeCount();

        if (MapUtil.getInt(typeCountMap, R_123, 0) <= 5) {
            return MutablePair.of(R_123, StrUtil.removePrefix(mediaPath, "http://192.168.1.249:5244/d/123"));
        }
        return MutablePair.of(L_NC2O, "");
    }

    private MutablePair<CloudStorageType, String> handleZong123Path(String mediaPath) {
        Map<CloudStorageType, Integer> typeCountMap = getCloudStorageTypeCount();

        if (MapUtil.getInt(typeCountMap, R_123_ZONG, 0) <= 5) {
            return MutablePair.of(R_123_ZONG, StrUtil.removePrefix(mediaPath,
                    "http://192.168.1.249:5244/d/zong123"));
        }
        return MutablePair.of(L_NC2O, "");
    }

    private Map<CloudStorageType, Integer> getCloudStorageTypeCount() {
        Set<String> deviceIds = redisClient.scanKeysByPrefix(THREAD_LIMIT_KEY);
        Map<CloudStorageType, Integer> typeCountMap = MapUtil.newHashMap();

        for (String deviceId : deviceIds) {
            CloudStorageType cloudStorageType = CloudStorageType.of(
                    CollUtil.getLast(SplitUtil.splitTrim(deviceId, "|")));
            typeCountMap.put(cloudStorageType, typeCountMap.getOrDefault(cloudStorageType, 0) + 1);
        }
        return typeCountMap;
    }

/*public synchronized MutablePair<CloudStorageType, String> limitThreadCache(String mediaPath) {
        if (StrUtil.containsAny(mediaPath, "/d/new115/")) {
            Set<String> deviceIds = redisClient.scanKeysByPrefix(THREAD_LIMIT_KEY);
            Map<CloudStorageType, Integer> typeCountMap = MapUtil.newHashMap();
            for (String deviceId : deviceIds) {
                CloudStorageType cloudStorageType = CloudStorageType.of(
                        CollUtil.getLast(SplitUtil.splitTrim(deviceId, "|")));
                typeCountMap.put(cloudStorageType, typeCountMap.getOrDefault(cloudStorageType, 0) + 1);
            }

            if (MapUtil.getInt(typeCountMap, R_115, 0) <= 3) {
                return MutablePair.of(R_115, StrUtil.removePrefix(mediaPath, "http://192.168.1.249:5244/d/new115"));

            }
            if (MapUtil.getInt(typeCountMap, R_123_ZONG, 0) <= 4) {
                return MutablePair.of(R_123_ZONG, StrUtil.removePrefix(mediaPath, "http://192.168.1.249:5244/d/new115"));
            }
            return MutablePair.of(L_NC2O, "");
        }
        if (StrUtil.contains(mediaPath, "/d/123/")) {
            Set<String> deviceIds = redisClient.scanKeysByPrefix(THREAD_LIMIT_KEY);
            Map<CloudStorageType, Integer> typeCountMap = MapUtil.newHashMap();
            for (String deviceId : deviceIds) {
                CloudStorageType cloudStorageType = CloudStorageType.of(
                        CollUtil.getLast(SplitUtil.splitTrim(deviceId, "|")));
                typeCountMap.put(cloudStorageType, typeCountMap.getOrDefault(cloudStorageType, 0) + 1);
            }
            if (MapUtil.getInt(typeCountMap, R_123, 0) <= 4) {
                return MutablePair.of(R_123, StrUtil.removePrefix(mediaPath, "http://192.168.1.249:5244/d/123"));
            }
            return MutablePair.of(L_NC2O, "");
        }
        return MutablePair.of(L_NC2O, "");
    }*/

}