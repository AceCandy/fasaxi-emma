package cn.acecandy.fasaxi.emma.utils;


import cn.hutool.v7.core.text.StrUtil;
import lombok.SneakyThrows;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * lock 工具类
 *
 * @author tangningzhu
 * @since 2024/10/16
 */
public final class LockUtil extends cn.hutool.v7.core.thread.lock.LockUtil {
    private static final String LOCK_VIDEO_CACHE_KEY = "lock:video-cache:{}";

    // 视频重定向------------------------------------------------------------------------------------------
    private static final Map<String, ReentrantLock> VIDEO_LOCK_CACHE_MAP =
            new ConcurrentHashMap<>();

    private LockUtil() {
    }

    /**
     * 是否锁定
     *
     * @param lock 锁
     * @return boolean
     */
    @SneakyThrows
    public static boolean isLock(Lock lock) {
        return !lock.tryLock();
    }

    /**
     * 是否锁定 等待expire秒
     *
     * @param lock 锁
     * @return boolean
     */
    @SneakyThrows
    public static boolean isLock(Lock lock, Integer expire) {
        return !lock.tryLock(expire, TimeUnit.SECONDS);
    }

    public static String buildVideoCacheLock(String itemId) {
        return StrUtil.format(LOCK_VIDEO_CACHE_KEY, itemId);
    }

    public static ReentrantLock lockVideoCache(String itemId) {
        return VIDEO_LOCK_CACHE_MAP.computeIfAbsent(buildVideoCacheLock(itemId), k -> new ReentrantLock());
    }

    public static void unlockVideoCache(ReentrantLock lock, String itemId) {
        if (lock != null && lock.isHeldByCurrentThread()) {
            try {
                lock.unlock(); // 现在这个unlock调用是在当前线程确实持有锁的情况下执行的
            } finally {
                VIDEO_LOCK_CACHE_MAP.remove(buildVideoCacheLock(itemId));
            }
        }
    }

}