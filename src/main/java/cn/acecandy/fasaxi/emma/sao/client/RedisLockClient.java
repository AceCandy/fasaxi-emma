package cn.acecandy.fasaxi.emma.sao.client;

import cn.acecandy.fasaxi.emma.common.enums.EmbyPicType;
import cn.acecandy.fasaxi.emma.config.EmbyContentCacheReqWrapper;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

/**
 * redis客户端
 *
 * @author AceCandy
 * @since 2025/05/03
 */
@Slf4j
@Component
public class RedisLockClient {

    private static final DefaultRedisScript<Long> COMPARE_AND_DELETE_SCRIPT =
            new DefaultRedisScript<>(
                    "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
                    Long.class);

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private final ThreadLocal<Map<String, String>> ownedLocks = ThreadLocal.withInitial(HashMap::new);

    /**
     * 播放session锁
     */
    private static final String LOCK_SESSIONS_KEY = "lock:sessions:{}|{}";

    public static String buildSessionsLock(EmbyContentCacheReqWrapper request) {
        String body = request.getCachedBody();
        return StrUtil.format(LOCK_SESSIONS_KEY, request.getRequestURI(),
                JSONUtil.parseObj(body).getStr("PlaySessionId"));
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
        String token = UUID.randomUUID().toString();
        try {
            Boolean success = redisTemplate.opsForValue().setIfAbsent(key, token, expire, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(success)) {
                ownedLocks.get().put(key, token);
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new IllegalStateException(StrUtil.format("获取Redis锁失败: {}", key), e);
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
        Map<String, String> currentLocks = ownedLocks.get();
        String token = currentLocks.remove(key);
        if (StrUtil.isBlank(token)) {
            if (currentLocks.isEmpty()) {
                ownedLocks.remove();
            }
            return;
        }
        try {
            redisTemplate.execute(COMPARE_AND_DELETE_SCRIPT, Collections.singletonList(key), token);
        } catch (Exception e) {
            log.error("释放Redis锁失败: {}", key, e);
        } finally {
            if (currentLocks.isEmpty()) {
                ownedLocks.remove();
            }
        }
    }

}
