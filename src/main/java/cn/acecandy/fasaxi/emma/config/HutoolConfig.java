package cn.acecandy.fasaxi.emma.config;

import org.dromara.hutool.http.client.engine.ClientEngine;
import org.dromara.hutool.http.client.engine.ClientEngineFactory;
import org.dromara.hutool.http.client.engine.okhttp.OkHttpClientConfig;
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
        // return ClientEngineFactory.createEngine(ApacheHttpClientConfig.of()
        //         .setMaxTotal(200).setMaxPerRoute(20).setConnectionTimeout(10_000).setReadTimeout(60_000));
        return ClientEngineFactory.createEngine(OkHttpClientConfig.of()
                .setMaxIdle(200).setConnectionTimeout(10_000).setReadTimeout(60_000));
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
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(RedisSerializer.string());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /*@Bean
    public RedisSerializer<Object> redisSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        //必须设置，否则无法将JSON转化为对象，会转化成Map类型
        // objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);

        // 自定义ObjectMapper的时间处理模块
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern("HH:mm:ss")));
        javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern("HH:mm:ss")));

        objectMapper.registerModule(javaTimeModule);

        // 禁用将日期序列化为时间戳的行为
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        //创建JSON序列化器
        return new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
    }*/
}