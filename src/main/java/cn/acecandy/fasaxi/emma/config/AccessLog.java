package cn.acecandy.fasaxi.emma.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.date.DateUtil;
import org.dromara.hutool.core.thread.ThreadUtil;
import org.springframework.stereotype.Component;

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
            log.debug("[{}:{}({}ms)] {}", req.getMethod(), res.getStatus(), DateUtil.current() - start,
                    req.getRequestURI() + "?" + req.getQueryString());
        });
    }
}