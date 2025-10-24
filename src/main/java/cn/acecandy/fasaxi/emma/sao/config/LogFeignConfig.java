package cn.acecandy.fasaxi.emma.sao.config;

import feign.Logger;
import feign.Response;
import org.springframework.context.annotation.Bean;

import java.io.IOException;

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

    @Bean
    public Logger feignLogger() {
        return new TimedFeignLogger();
    }

    public class TimedFeignLogger extends Logger {
        @Override
        protected void log(String configKey, String format, Object... args) {
            // 默认日志打印（可自定义格式）
            System.out.printf("[%s] %s%n", configKey, String.format(format, args));
        }

        @Override
        protected Response logAndRebufferResponse(
                String configKey, Level logLevel, Response response, long elapsedTime
        ) throws IOException {
            // elapsedTime 就是Feign内置计算的耗时（毫秒）
            log(configKey, "Feign请求耗时：%d ms", elapsedTime);
            return super.logAndRebufferResponse(configKey, logLevel, response, elapsedTime);
        }
    }
}