package cn.acecandy.fasaxi.emma.utils;

import cn.acecandy.fasaxi.emma.common.enums.CloudStorageType;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.hutool.v7.core.lang.mutable.MutablePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThreadLimitUtilTest {

    @Mock
    private RedisClient redisClient;

    private ThreadLimitUtil threadLimitUtil;

    @BeforeEach
    void setUp() {
        threadLimitUtil = new ThreadLimitUtil();
        ReflectionTestUtils.setField(threadLimitUtil, "redisClient", redisClient);
    }

    @Test
    void setThreadCache_shouldMoveDeviceToNewGroupWithoutScanningKeys() {
        when(redisClient.getStr("cache:a-thread-limit:device:device-1")).thenReturn("115");

        threadLimitUtil.setThreadCache(CloudStorageType.R_123_ZONG, "device-1");

        verify(redisClient).zrem("cache:a-thread-limit:group:115", "device-1");
        verify(redisClient).set("cache:a-thread-limit:device:device-1", "123_zong", 7200);
        verify(redisClient).zadd(eq("cache:a-thread-limit:group:123_zong"), eq("device-1"), anyDouble());
        verify(redisClient).expire("cache:a-thread-limit:group:123_zong", 7200);
        verify(redisClient, never()).scanKeysByPrefix(anyString());
    }

    @Test
    void limitThreadCache_new115Path_shouldReturnRelativePathForZong123Fallback() {
        when(redisClient.zcard("cache:a-thread-limit:group:115")).thenReturn(4);
        when(redisClient.zcard("cache:a-thread-limit:group:123_zong")).thenReturn(2);

        MutablePair<CloudStorageType, String> result = threadLimitUtil.limitThreadCache(
                "http://192.168.1.249:5244/d/new115/library/movie/test.mkv");

        assertEquals(CloudStorageType.R_123_ZONG, result.getLeft());
        assertEquals("/library/movie/test.mkv", result.getRight());
        verify(redisClient).zremrangeByScore(eq("cache:a-thread-limit:group:115"), anyDouble(), anyDouble());
        verify(redisClient).zremrangeByScore(eq("cache:a-thread-limit:group:123_zong"), anyDouble(), anyDouble());
        verify(redisClient, never()).scanKeysByPrefix(anyString());
    }

    @Test
    void removeThreadCache_shouldDeleteDeviceStateAndGroupMembership() {
        when(redisClient.getStr("cache:a-thread-limit:device:device-1")).thenReturn("123");

        threadLimitUtil.removeThreadCache("device-1");

        verify(redisClient).zrem("cache:a-thread-limit:group:123", "device-1");
        verify(redisClient).del("cache:a-thread-limit:device:device-1");
    }
}
