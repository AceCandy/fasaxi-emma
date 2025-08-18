package cn.acecandy.fasaxi.emma.sao.config;

import feign.Logger;
import org.springframework.context.annotation.Bean;

/**
 * 日志打印
 *
 * @author tangningzhu
 * @since 2025/8/18
 */
public class LogFeignConfig {

    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

}