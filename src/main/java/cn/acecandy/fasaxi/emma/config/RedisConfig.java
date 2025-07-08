package cn.acecandy.fasaxi.emma.config;

import org.noear.redisx.RedisClient;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;

/**
 * redis配置
 *
 * @author tangningzhu
 * @since 2025/7/7
 */
@Configuration
public class RedisConfig {
    @Bean
    public RedisClient redisClientBase(@Inject("${rs1}") RedisClient client) {
        return client;
    }
}