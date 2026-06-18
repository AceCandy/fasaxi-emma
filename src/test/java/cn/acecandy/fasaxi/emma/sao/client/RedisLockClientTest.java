package cn.acecandy.fasaxi.emma.sao.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisLockClientTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RedisLockClient redisLockClient;

    @BeforeEach
    void setUp() {
        redisLockClient = new RedisLockClient();
        ReflectionTestUtils.setField(redisLockClient, "redisTemplate", redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void lockAndUnlock_shouldUseOwnerTokenAndCompareDelete() {
        when(valueOperations.setIfAbsent(eq("lock:test"), anyString(), anyLong(), eq(TimeUnit.SECONDS)))
                .thenReturn(true);

        assertTrue(redisLockClient.lock("lock:test", 30));
        redisLockClient.unlock("lock:test");

        verify(redisTemplate).execute(any(), eq(Collections.singletonList("lock:test")), anyString());
        verify(redisTemplate, never()).delete("lock:test");
    }

    @Test
    void lock_redisUnavailable_shouldThrowIllegalState() {
        when(valueOperations.setIfAbsent(eq("lock:test"), anyString(), anyLong(), eq(TimeUnit.SECONDS)))
                .thenThrow(new RuntimeException("redis down"));

        assertThrows(IllegalStateException.class, () -> redisLockClient.lock("lock:test", 30));
    }
}
