package cn.acecandy.fasaxi.emma.task;

import cn.acecandy.fasaxi.emma.task.impl.CollectionTaskService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
    // @Scheduled(fixedDelay = 11 * 60, timeUnit = TimeUnit.MINUTES, initialDelay = 10)
    public void syncCollection() {
        try {
            collectionTaskService.syncCollection();
        } catch (Exception e) {
            log.error("执行异常-同步自定义合集 ", e);
        }
    }

}