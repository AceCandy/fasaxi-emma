package cn.acecandy.fasaxi.emma.config;

import io.undertow.server.DefaultByteBufferPool;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;

/**
 * 所有系统配置
 *
 * @author tangningzhu
 * @since 2025/4/15
 */
@Configuration
public class GodConfig {

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

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(RedisSerializer.string());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public WebServerFactoryCustomizer<UndertowServletWebServerFactory> undertowCustomizer() {
        return factory ->
                factory.addDeploymentInfoCustomizers(deploymentInfo -> {
                    // 创建自定义缓冲区池 不使用直接内存（堆外内存）缓冲区1024kb 最大缓冲区数量100 池最大容量10m 线程局部换成32
                    DefaultByteBufferPool bufferPool = new DefaultByteBufferPool(
                            false, 1024, 100, 10, 32
                    );
                    // 获取或创建 WebSocket 部署信息
                    WebSocketDeploymentInfo webSocketDeploymentInfo =
                            (WebSocketDeploymentInfo) deploymentInfo.getServletContextAttributes()
                                    .computeIfAbsent(WebSocketDeploymentInfo.ATTRIBUTE_NAME, k -> new WebSocketDeploymentInfo());
                    // 设置缓冲区池
                    webSocketDeploymentInfo.setBuffers(bufferPool);
                });
    }

    @Bean("cacheRefreshExecutor")
    public Executor cacheRefreshExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("cache-refresh-");
        executor.initialize();
        return executor;
    }
}