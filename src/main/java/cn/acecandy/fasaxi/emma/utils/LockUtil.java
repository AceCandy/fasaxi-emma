package cn.acecandy.fasaxi.emma.utils;


import cn.acecandy.fasaxi.emma.common.enums.EmbyPicType;
import cn.acecandy.fasaxi.emma.config.EmbyContentCacheReqWrapper;
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
    private static final String LOCK_VIDEO_KEY = "lock:video:{}";
    private static final String LOCK_VIDEO_CACHE_KEY = "lock:video-cache:{}";
    // private static final Map<String, Lock> VIDEO_LOCK_MAP = MapUtil.newSafeConcurrentHashMap();
    private static final Map<String, Lock> VIDEO_LOCK_MAP = new ConcurrentHashMap<>();

    // 视频重定向------------------------------------------------------------------------------------------
    private static final Map<String, Lock> VIDEO_LOCK_CACHE_MAP =
            new ConcurrentHashMap<>();
    // 原始请求------------------------------------------------------------------------------------------
    private static final String LOCK_ORIGIN_KEY = "lock:origin:{}|{}";
    private static final Map<String, Lock> ORIGIN_LOCK_MAP =
            new ConcurrentHashMap<>();
    // 用户权限请求------------------------------------------------------------------------------------------
    private static final String LOCK_USER_PERMS_KEY = "lock:user-perms:{}";
    private static final Map<String, Lock> USER_PERMS_LOCK_MAP =
            new ConcurrentHashMap<>();
    // 库中媒体项请求------------------------------------------------------------------------------------------
    private static final String LOCK_ITEMS_KEY = "lock:items:{}";
    private static final Map<String, Lock> ITEMS_LOCK_MAP =
            new ConcurrentHashMap<>();
    // 设备临时文件请求------------------------------------------------------------------------------------------
    private static final String LOCK_DEVICE_KEY = "lock:device:{}";
    private static final Map<String, Lock> DEVICE_LOCK_MAP =
            new ConcurrentHashMap<>();
    private static final String LOCK_PIC_KEY = "lock:pic:{}|{}";
    private static final Map<String, Lock> PIC_LOCK_MAP =
            new ConcurrentHashMap<>();
    private static final String LOCK_REFRESH_TMDB_KEY = "lock:refresh-tmdb:{}";
    private static final String LOCK_REFRESH_MEDIA_KEY = "lock:refresh-media:{}";

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
     * 是否锁定 等待1秒
     *
     * @param lock 锁
     * @return boolean
     */
    @SneakyThrows
    public static boolean isLock1s(Lock lock) {
        return !lock.tryLock(1, TimeUnit.SECONDS);
    }

    private static String buildVideoLock(String mediaSourceId) {
        return StrUtil.format(LOCK_VIDEO_KEY, mediaSourceId);
    }

    public static Lock lockVideo(String mediaSourceId) {
        return VIDEO_LOCK_MAP.computeIfAbsent(buildVideoLock(mediaSourceId), k -> new ReentrantLock());
    }

    public static void unlockVideo(Lock lock, String mediaSourceId) {
        if (null != lock) {
            lock.unlock();
        }
        VIDEO_LOCK_MAP.remove(buildVideoLock(mediaSourceId));
    }

    public static String buildVideoCacheLock(String itemId) {
        return StrUtil.format(LOCK_VIDEO_CACHE_KEY, itemId);
    }

    public static Lock lockVideoCache(String itemId) {
        return VIDEO_LOCK_CACHE_MAP.computeIfAbsent(buildVideoCacheLock(itemId), k -> new ReentrantLock());
    }

    public static void unlockVideoCache(Lock lock, String itemId) {
        if (null != lock) {
            lock.unlock();
        }
        VIDEO_LOCK_CACHE_MAP.remove(buildVideoCacheLock(itemId));
    }

    private static String buildOriginLock(EmbyContentCacheReqWrapper request) {
        return StrUtil.format(LOCK_ORIGIN_KEY, request.getRequestURI(), request.getCachedParam());
    }

    public static Lock lockOrigin(EmbyContentCacheReqWrapper request) {
        return ORIGIN_LOCK_MAP.computeIfAbsent(buildOriginLock(request), k -> new ReentrantLock());
    }

    public static void unlockOrigin(Lock lock, EmbyContentCacheReqWrapper request) {
        if (null != lock) {
            lock.unlock();
        }
        ORIGIN_LOCK_MAP.remove(buildOriginLock(request));
    }

    private static String buildUserPermsLock(String userId) {
        return StrUtil.format(LOCK_USER_PERMS_KEY, userId);
    }

    public static Lock lockUserPerms(String userId) {
        return ORIGIN_LOCK_MAP.computeIfAbsent(buildUserPermsLock(userId), k -> new ReentrantLock());
    }

    public static void unlockUserPerms(Lock lock, String userId) {
        if (null != lock) {
            lock.unlock();
        }
        USER_PERMS_LOCK_MAP.remove(buildUserPermsLock(userId));
    }

    private static String buildItemsLock(String parentId) {
        return StrUtil.format(LOCK_ITEMS_KEY, parentId);
    }

    public static Lock lockItems(String parentId) {
        return ITEMS_LOCK_MAP.computeIfAbsent(buildItemsLock(parentId), k -> new ReentrantLock());
    }

    // 图片请求-------------------------------------------------------------------------------------------

    public static void unlockItems(Lock lock, String parentId) {
        if (null != lock) {
            lock.unlock();
        }
        ITEMS_LOCK_MAP.remove(buildItemsLock(parentId));
    }

    private static String buildDeviceLock(String deviceId) {
        return StrUtil.format(LOCK_DEVICE_KEY, deviceId);
    }

    public static Lock lockDeviceLock(String deviceId) {
        return DEVICE_LOCK_MAP.computeIfAbsent(buildDeviceLock(deviceId), k -> new ReentrantLock());
    }

    public static void unlockDevice(Lock lock, String deviceId) {
        if (null != lock) {
            lock.unlock();
        }
        ORIGIN_LOCK_MAP.remove(buildDeviceLock(deviceId));
    }

    private static String buildPicLock(String mediaSourceId, EmbyPicType picType) {
        return StrUtil.format(LOCK_PIC_KEY, mediaSourceId, picType.getValue());
    }

    // tmdb信息更新-------------------------------------------------------------------------------------------

    public static Lock lockPic(String mediaSourceId, EmbyPicType picType) {
        return ORIGIN_LOCK_MAP.computeIfAbsent(buildPicLock(mediaSourceId, picType), k -> new ReentrantLock());
    }

    public static void unlockPic(Lock lock, String mediaSourceId, EmbyPicType picType) {
        if (null != lock) {
            lock.unlock();
        }
        ORIGIN_LOCK_MAP.remove(buildPicLock(mediaSourceId, picType));
    }

    public static String buildRefreshTmdbLock(String itemId) {
        return StrUtil.format(LOCK_REFRESH_TMDB_KEY, itemId);
    }

    public static String buildRefreshMediaLock(String itemId) {
        return StrUtil.format(LOCK_REFRESH_MEDIA_KEY, itemId);
    }

}