package cn.acecandy.fasaxi.emma.task;

import cn.acecandy.fasaxi.emma.task.impl.TmdbProviderTaskService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * tmdb 外部id相关 定时任务
 *
 * @author tangningzhu
 * @since 2025/3/3
 */
@Slf4j
@Component
public class TmdbProviderTask {

    @Resource
    private TmdbProviderTaskService tmdbProviderTaskService;

    @Value("${task.all-enabled:true}")
    private boolean allEnabled;

    /**
     * 同步TmdbProvider
     */
    @Scheduled(fixedDelay = 20, timeUnit = TimeUnit.MINUTES, initialDelay = 125)
    public void syncTmdbProvider() {
        try {
            if (!allEnabled) {
                return;
            }
            tmdbProviderTaskService.syncTmdbProvider(null, null);
        } catch (Exception e) {
            log.error("执行异常-同步TmdbProvider ", e);
        }
    }

    /**
     * 补充缺失的豆瓣id
     */
    @Scheduled(fixedDelay = 12, timeUnit = TimeUnit.MINUTES, initialDelay = 65)
    public void completionDoubanId() {
        try {
            if (!allEnabled) {
                return;
            }
            tmdbProviderTaskService.completionDoubanId();
        } catch (Exception e) {
            log.error("执行异常-补充缺失的豆瓣id ", e);
        }
    }

}