package cn.acecandy.fasaxi.emma.task;

import cn.acecandy.fasaxi.emma.task.impl.Webdav2CloudTaskService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * webdav资源上传到云 定时任务
 *
 * @author tangningzhu
 * @since 2025/3/3
 */
@Slf4j
@Component
public class Webdav2CloudTask {

    @Resource
    private Webdav2CloudTaskService webdav2CloudTaskService;

    /**
     * micu上传到115
     */
    @Scheduled(
            initialDelay = 1,       // 启动后1分钟执行第一次
            fixedDelay = 2,         // 每次执行完后间隔5分钟再执行
            timeUnit = TimeUnit.MINUTES  // 时间单位为分钟
    )
    public void webdav2Cloud() {
        try {
            webdav2CloudTaskService.uploadTo115();
        } catch (Exception e) {
            log.error("执行异常-micu上传到115 ", e);
        }
    }

    /**
     * 115上传到123
     */
    @Scheduled(
            initialDelay = 3,       // 启动后1分钟执行第一次
            fixedDelay = 5,         // 每次执行完后间隔5分钟再执行
            timeUnit = TimeUnit.MINUTES  // 时间单位为分钟
    )
    public void r115to123() {
        try {
            webdav2CloudTaskService.r115to123();
        } catch (Exception e) {
            log.error("执行异常-115上传到123 ", e);
        }
    }

}