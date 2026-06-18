package cn.acecandy.fasaxi.emma.sao.config;

import cn.acecandy.fasaxi.emma.utils.LogSanitizer;
import feign.Logger;
import feign.Response;
import org.slf4j.LoggerFactory;
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

    public static class TimedFeignLogger extends Logger {
        private static final org.slf4j.Logger log = LoggerFactory.getLogger(TimedFeignLogger.class);

        @Override
        protected void log(String configKey, String format, Object... args) {
            String message = (args == null || args.length == 0) ? format : String.format(format, args);
            log.debug("[{}] {}", configKey, LogSanitizer.sanitizeTextForLog(message));
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
