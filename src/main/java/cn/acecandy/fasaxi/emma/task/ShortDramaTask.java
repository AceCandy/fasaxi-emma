package cn.acecandy.fasaxi.emma.task;

import cn.acecandy.fasaxi.emma.task.impl.ShortDramaTaskService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 短剧相关 定时任务
 *
 * @author tangningzhu
 * @since 2025/3/3
 */
@Slf4j
@Component
public class ShortDramaTask {

    @Resource
    private ShortDramaTaskService shortDramaTaskService;

    @Value("${task.all-enabled:true}")
    private boolean allEnabled;

    /**
     * 同步短剧媒体项
     */
    @Scheduled(fixedDelay = 77, timeUnit = TimeUnit.MINUTES, initialDelay = 5)
    public void syncItems() {
        try {
            if (!allEnabled) {
                return;
            }
            shortDramaTaskService.syncItemInfo();
        } catch (Exception e) {
            log.error("执行异常-同步短剧媒体项 ", e);
        }
    }

    /**
     * 同步jcc短剧榜单数据
     */
    @Scheduled(fixedDelay = 11 * 60, timeUnit = TimeUnit.MINUTES, initialDelay = 7)
    public void syncJccRanks() {
        try {
            if (!allEnabled) {
                return;
            }
            shortDramaTaskService.syncJccRanks();
        } catch (Exception e) {
            log.error("执行异常-同步jcc短剧榜单数据 ", e);
        }
    }

}