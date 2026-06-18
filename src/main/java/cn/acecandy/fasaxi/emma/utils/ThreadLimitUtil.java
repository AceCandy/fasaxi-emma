package cn.acecandy.fasaxi.emma.utils;

import cn.acecandy.fasaxi.emma.common.enums.CloudStorageType;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.hutool.v7.core.lang.mutable.MutablePair;
import cn.hutool.v7.core.text.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static cn.acecandy.fasaxi.emma.common.enums.CloudStorageType.L_NC2O;
import static cn.acecandy.fasaxi.emma.common.enums.CloudStorageType.R_115;
import static cn.acecandy.fasaxi.emma.common.enums.CloudStorageType.R_123;
import static cn.acecandy.fasaxi.emma.common.enums.CloudStorageType.R_123_ZONG;
import static cn.acecandy.fasaxi.emma.utils.CacheUtil.buildThreadLimitDeviceStateKey;
import static cn.acecandy.fasaxi.emma.utils.CacheUtil.buildThreadLimitGroupKey;

/**
 * emby 工具类
 *
 * @author tangningzhu
 * @since 2024/10/16
 */
@Slf4j
@Component
public class ThreadLimitUtil {
    /**
     * EXP时间 2hour 防止重启导致的key丢失
     */
    private final static Integer EXP_TIME = 60 * 60 * 2;
    private static final String NEW115_MARKER = "/d/new115";
    private static final String PATH_123_MARKER = "/d/123";
    private static final String PATH_ZONG123_MARKER = "/d/zong123";
    @Resource
    private RedisClient redisClient;

    /**
     * 设置线程缓存
     *
     * @param cloudStorageType 云盘类型 115/123
     * @param deviceId         设备ID
     */
    public void setThreadCache(CloudStorageType cloudStorageType, String deviceId) {
        if (cloudStorageType == null || StrUtil.isBlank(deviceId)) {
            return;
        }
        String deviceStateKey = buildThreadLimitDeviceStateKey(deviceId);
        String previousType = redisClient.getStr(deviceStateKey);
        CloudStorageType previousCloudStorageType = CloudStorageType.of(previousType);
        if (previousCloudStorageType != null && previousCloudStorageType != cloudStorageType) {
            redisClient.zrem(buildThreadLimitGroupKey(previousCloudStorageType), deviceId);
        }

        long expireAt = System.currentTimeMillis() + EXP_TIME * 1000L;
        redisClient.set(deviceStateKey, cloudStorageType.getValue(), EXP_TIME);
        redisClient.zadd(buildThreadLimitGroupKey(cloudStorageType), deviceId, expireAt);
        redisClient.expire(buildThreadLimitGroupKey(cloudStorageType), EXP_TIME);
    }

    /**
     * 移除线程缓存
     *
     * @param deviceId 设备ID
     */
    public void removeThreadCache(String deviceId) {
        if (StrUtil.isBlank(deviceId)) {
            return;
        }
        String deviceStateKey = buildThreadLimitDeviceStateKey(deviceId);
        CloudStorageType cloudStorageType = CloudStorageType.of(redisClient.getStr(deviceStateKey));
        if (cloudStorageType != null) {
            redisClient.zrem(buildThreadLimitGroupKey(cloudStorageType), deviceId);
        }
        redisClient.del(deviceStateKey);
    }

    /**
     * 线程超限检测
     *
     * @return 返回输出类型 CloudStorageType
     */
    public MutablePair<CloudStorageType, String> limitThreadCache(String mediaPath) {
        if (StrUtil.containsAny(mediaPath, NEW115_MARKER + "/")) {
            return handleNew115Path(mediaPath);
        }
        if (StrUtil.contains(mediaPath, PATH_123_MARKER + "/")) {
            return handle123Path(mediaPath);
        }
        if (StrUtil.contains(mediaPath, PATH_ZONG123_MARKER + "/")) {
            return handleZong123Path(mediaPath);
        }
        return MutablePair.of(L_NC2O, "");
    }

    private MutablePair<CloudStorageType, String> handleNew115Path(String mediaPath) {
        String relativePath = extractRelativePath(mediaPath, NEW115_MARKER);

        if (countActiveDevices(R_115) <= 3) {
            return MutablePair.of(R_115, relativePath);
        }
        if (countActiveDevices(R_123_ZONG) <= 4) {
            return MutablePair.of(R_123_ZONG, relativePath);
        }
        return MutablePair.of(L_NC2O, "");
    }

    private MutablePair<CloudStorageType, String> handle123Path(String mediaPath) {
        if (countActiveDevices(R_123) <= 5) {
            return MutablePair.of(R_123, extractRelativePath(mediaPath, PATH_123_MARKER));
        }
        return MutablePair.of(L_NC2O, "");
    }

    private MutablePair<CloudStorageType, String> handleZong123Path(String mediaPath) {
        if (countActiveDevices(R_123_ZONG) <= 5) {
            return MutablePair.of(R_123_ZONG, extractRelativePath(mediaPath, PATH_ZONG123_MARKER));
        }
        return MutablePair.of(L_NC2O, "");
    }

    /**
     * 进入播放热路径前，先清除过期设备，再按云盘直接取当前活跃数量，避免全量扫描 Redis key。
     */
    private int countActiveDevices(CloudStorageType cloudStorageType) {
        if (cloudStorageType == null) {
            return 0;
        }
        String groupKey = buildThreadLimitGroupKey(cloudStorageType);
        redisClient.zremrangeByScore(groupKey, Double.NEGATIVE_INFINITY, System.currentTimeMillis());
        return redisClient.zcard(groupKey);
    }

    private String extractRelativePath(String mediaPath, String marker) {
        if (StrUtil.hasBlank(mediaPath, marker)) {
            return mediaPath;
        }
        int markerIndex = mediaPath.toLowerCase().indexOf(marker.toLowerCase());
        if (markerIndex < 0) {
            return mediaPath;
        }
        String relativePath = StrUtil.subSuf(mediaPath, markerIndex + marker.length());
        if (StrUtil.isBlank(relativePath)) {
            return "/";
        }
        return StrUtil.startWith(relativePath, "/") ? relativePath : "/" + relativePath;
    }

}
