package cn.acecandy.fasaxi.emma.task;

import cn.acecandy.fasaxi.emma.task.impl.CollectionTaskService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 自定义合集 定时任务
 *
 * @author tangningzhu
 * @since 2025/3/3
 */
@Slf4j
@Component
public class CollectionTask {

    @Resource
    private CollectionTaskService collectionTaskService;

    /**
     * 同步自定义合集
     */
    @Scheduled(fixedDelay = 14 * 60, timeUnit = TimeUnit.MINUTES, initialDelay = 30)
    public void syncQuickCollection() {
        try {
            collectionTaskService.syncQuickCollection();
        } catch (Exception e) {
            log.error("执行异常-同步自定义合集(快速) ", e);
        }
    }
}