package cn.acecandy.fasaxi.emma.task;

import cn.acecandy.fasaxi.emma.task.impl.JavTaskService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * jav相关 定时任务
 *
 * @author tangningzhu
 * @since 2025/3/3
 */
@Slf4j
@Component
public class JavTask {

    @Resource
    private JavTaskService javTaskService;

    @Value("${task.all-enabled:true}")
    private boolean allEnabled;

    /**
     * 同步javDB-有码-月榜
     */
    @Scheduled(fixedDelay = 20, timeUnit = TimeUnit.MINUTES, initialDelay = 65)
    public void syncJavDbCensoredMonth() {
        try {
            if (!allEnabled) {
                return;
            }
            javTaskService.syncJavDbCensoredMonth();
        } catch (Exception e) {
            log.error("执行异常-同步javDB-有码-月榜 ", e);
        }
    }

}