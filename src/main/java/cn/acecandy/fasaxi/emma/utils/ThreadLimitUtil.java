package cn.acecandy.fasaxi.emma.utils;

import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
     * @param type     云盘类型 115/123
     * @param deviceId 设备ID
     */
    public synchronized void setThreadCache(Integer type, String deviceId) {
        redisClient.set(CacheUtil.buildThreadLimitKey(deviceId), type, EXP_TIME);
        // 将设备ID添加到集合中，用于统计总数
        // redisClient.sadd(THREAD_LIMIT_KEY, deviceId);
    }

    /**
     * 移除线程缓存
     *
     * @param deviceId 设备ID
     */
    public synchronized void removeThreadCache(String deviceId) {
        // 删除设备ID与类型的映射
        redisClient.del(CacheUtil.buildThreadLimitKey(deviceId));
        // 从集合中移除设备ID
        // redisClient.srem(THREAD_LIMIT_KEY, deviceId);
    }

    /**
     * 检查线程是否超限
     *
     * @return 是否超限
     */
    public synchronized boolean limitThreadCache() {
        // 检查总数量是否超过6
        // int totalCount = redisClient.getscard(THREAD_LIMIT_KEY);
        int totalCount = CollUtil.size(redisClient.getStrOnScan(THREAD_LIMIT_KEY));
        if (totalCount > 6) {
            return true;
        }

        // 检查115类型是否超过4
        int type115Count = countByType(115);
        if (type115Count > 4) {
            return true;
        }

        // 检查123类型是否超过4
        int type123Count = countByType(123);
        return type123Count > 4;
    }

    /**
     * 统计指定类型的设备数量
     *
     * @param type 设备类型
     * @return 数量
     */
    private int countByType(Integer type) {
        // 获取所有设备ID
        Set<Object> deviceIds = redisClient.smembers(THREAD_LIMIT_KEY);
        if (CollUtil.isEmpty(deviceIds)) {
            return 0;
        }

        // 构建所有设备ID对应的key
        List<String> keys = deviceIds.stream()
                .map(id -> CacheUtil.buildThreadLimitKey(id.toString()))
                .collect(Collectors.toList());

        // 批量获取设备类型
        List<Object> types = redisClient.get(keys);

        // 统计指定类型的数量
        return CollUtil.count(types, type::equals);
    }


}