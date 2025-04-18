package cn.acecandy.fasaxi.emma.config;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author tangningzhu
 * @since 2025/4/15
 */
@Configuration
public class HttpPoolConfig {
    /**
     * 连接池管理器（最大200连接，每个路由50并发）
     *
     * @return {@link PoolingHttpClientConnectionManager }
     */
    @Bean
    public PoolingHttpClientConnectionManager poolingConnManager() {
        PoolingHttpClientConnectionManager manager =
                new PoolingHttpClientConnectionManager();
        manager.setMaxTotal(200);
        manager.setDefaultMaxPerRoute(50);
        return manager;
    }

    /**
     * 带连接池的HttpClient（全局单例）
     * <p>
     * 重试3次
     *
     * @return {@link CloseableHttpClient }
     */
    @Bean
    public CloseableHttpClient embyHttpClient() {
        return HttpClients.custom()
                .setConnectionManager(poolingConnManager())
                .setRetryHandler(new DefaultHttpRequestRetryHandler(3, true))
                .build();
    }

    /**
     * 定时任务线程池
     *
     * @return {@link TaskScheduler }
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("task-");
        return scheduler;
    }
}