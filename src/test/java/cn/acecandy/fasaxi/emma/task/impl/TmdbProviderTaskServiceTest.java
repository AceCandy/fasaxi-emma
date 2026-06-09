package cn.acecandy.fasaxi.emma.task.impl;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class TmdbProviderTaskServiceTest {

    @Test
    void newTmdbSyncExecutor_shouldUseBoundedQueueAndCallerRunsBackpressure() {
        ExecutorService executor = TmdbProviderTaskService.newTmdbSyncExecutor();
        try {
            ThreadPoolExecutor pool = assertInstanceOf(ThreadPoolExecutor.class, executor);

            assertEquals(5, pool.getCorePoolSize());
            assertEquals(5, pool.getMaximumPoolSize());
            assertEquals(100, pool.getQueue().remainingCapacity());
            assertInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class, pool.getRejectedExecutionHandler());
        } finally {
            executor.shutdownNow();
        }
    }
}
