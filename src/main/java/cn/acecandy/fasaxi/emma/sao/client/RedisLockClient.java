package cn.acecandy.fasaxi.emma.sao.client;

import cn.acecandy.fasaxi.emma.common.enums.EmbyPicType;
import cn.acecandy.fasaxi.emma.config.EmbyContentCacheReqWrapper;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * redis客户端
 *
 * @author AceCandy
 * @since 2025/05/03
 */
@Slf4j
@Component
public class RedisLockClient {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 播放session锁
     */
    private static final String LOCK_SESSIONS_KEY = "lock:sessions:{}";

    public static String buildSessionsLock(EmbyContentCacheReqWrapper request) {
        String body = request.getCachedBody();
        return StrUtil.format(LOCK_SESSIONS_KEY, JSONUtil.parseObj(body).getStr("PlaySessionId"));
    }

    /**
     * 图片请求
     */
    private static final String LOCK_PIC_KEY = "lock:pic:{}|{}";

    public static String buildPicLock(String mediaSourceId, EmbyPicType picType) {
        return StrUtil.format(LOCK_PIC_KEY, mediaSourceId, picType.getValue());
    }

    /**
     * 原始请求
     */
    private static final String LOCK_ORIGIN_KEY = "lock:origin:{}|{}";

    public static String buildOriginLock(EmbyContentCacheReqWrapper request) {
        return StrUtil.format(LOCK_ORIGIN_KEY, request.getRequestURI(), request.getCachedParam());
    }

    /**
     * 视频请求
     */
    private static final String LOCK_VIDEO_KEY = "lock:video:{}";

    public static String buildVideoLock(String mediaSourceId) {
        return StrUtil.format(LOCK_VIDEO_KEY, mediaSourceId);
    }

    /**
     * 全部媒体项
     */
    private static final String LOCK_ITEMS_KEY = "lock:items:{}";

    public static String buildItemsLock(String parentId) {
        return StrUtil.format(LOCK_ITEMS_KEY, parentId);
    }

    /**
     * 设备对应临时文件锁
     */
    private static final String LOCK_DEVICE_KEY = "lock:device:{}";

    public static String buildDeviceLock(String deviceId) {
        return StrUtil.format(LOCK_DEVICE_KEY, deviceId);
    }

    private static final String LOCK_REFRESH_MEDIA_KEY = "lock:refresh-media:{}";

    public static String buildRefreshMediaLock(String itemId) {
        return StrUtil.format(LOCK_REFRESH_MEDIA_KEY, itemId);
    }

    /**
     * 锁
     *
     * @param key    钥匙
     * @param expire 过期
     * @return boolean
     */
    public boolean lock(String key, Integer expire) {
        if (StrUtil.isBlank(key)) {
            return true;
        }
        try {
            Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "1", expire, TimeUnit.SECONDS);
            // 处理Redis不可用的情况（返回true，允许请求执行，避免业务阻断）
            return success == null || success;
        } catch (Exception e) {
            // Redis异常时，默认放行请求（可根据需求改为抛出异常或返回false）
            return true;
        }
    }

    /**
     * 锁
     * <p>
     * 默认65s过期
     *
     * @param key 钥匙
     * @return boolean
     */
    public boolean lock(String key) {
        if (StrUtil.isBlank(key)) {
            return true;
        }
        return lock(key, 65);
    }

    /**
     * 解锁
     *
     * @param key 钥匙
     */
    public void unlock(String key) {
        if (StrUtil.isBlank(key)) {
            return;
        }
        redisTemplate.delete(key);
    }

}