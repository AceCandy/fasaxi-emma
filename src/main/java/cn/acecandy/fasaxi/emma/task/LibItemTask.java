package cn.acecandy.fasaxi.emma.task;

import cn.acecandy.fasaxi.emma.task.impl.LibItemTaskService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 库媒体项 定时任务
 *
 * @author tangningzhu
 * @since 2025/3/3
 */
@Slf4j
@Component
public class LibItemTask {

    @Resource
    private LibItemTaskService libItemTaskService;

    @Value("${task.all-enabled:true}")
    private boolean allEnabled;

    /**
     * 同步库的媒体项
     */
    @Scheduled(fixedDelay = 11 * 60, timeUnit = TimeUnit.MINUTES, initialDelay = 10)
    public void syncLibItems() {
        try {
            if (!allEnabled) {
                return;
            }
            libItemTaskService.syncLibItems();
        } catch (Exception e) {
            log.error("执行异常-同步库的媒体项 ", e);
        }
    }

}