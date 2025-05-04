package cn.acecandy.fasaxi.emma.config;

import org.dromara.hutool.http.client.ApacheHttpClientConfig;
import org.dromara.hutool.http.client.engine.ClientEngine;
import org.dromara.hutool.http.client.engine.ClientEngineFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author tangningzhu
 * @since 2025/4/15
 */
@Configuration
public class HutoolConfig {

    @Bean
    public ClientEngine httpClient() {
        return ClientEngineFactory.createEngine(ApacheHttpClientConfig.of()
                .setMaxTotal(200).setMaxPerRoute(20).setTimeout(15_000));
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

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        // Key使用String序列化
        // template.setKeySerializer(new StringRedisSerializer());
        // Value使用Jackson序列化
        // template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}