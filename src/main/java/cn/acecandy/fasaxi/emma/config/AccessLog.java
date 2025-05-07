package cn.acecandy.fasaxi.emma.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.date.DateUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.core.thread.ThreadUtil;
import org.springframework.stereotype.Component;

import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_200;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_300;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_400;

/**
 * 请求日志记录
 *
 * @author AceCandy
 * @since 2024/11/19
 */
@Slf4j
@Component
public class AccessLog {

    public void log(HttpServletRequest req, HttpServletResponse res, long start) {
        ThreadUtil.execAsync(() -> {
            ThreadUtil.execAsync(() -> {
                int statusCode = res.getStatus();
                long elapsedTime = DateUtil.current() - start;
                String logMessage = StrUtil.format("[{}-{}:{}ms] {}", statusCode,
                        req.getMethod(), elapsedTime, req.getRequestURI(), req.getQueryString());
                if (statusCode >= CODE_200 && statusCode < CODE_300) {
                    log.info(logMessage);
                } else if (statusCode >= CODE_300 && statusCode < CODE_400) {
                    log.warn(logMessage);
                } else {
                    log.error(logMessage);
                }
            });
        });
    }
}