package cn.acecandy.fasaxi.emma.config;

import cn.acecandy.fasaxi.emma.utils.LogSanitizer;
import cn.acecandy.fasaxi.emma.utils.ThreadUtil;
import cn.hutool.v7.core.date.DateUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 请求日志记录
 *
 * @author AceCandy
 * @since 2024/11/19
 */
@Slf4j
@Component
public class AccessLog {

    public void log(String method, String uri, String ip, String queryStr,
                    Map<String, String> cachedHeader,
                    int status, long start) {
        ThreadUtil.execute(() -> {
            String sanitizedQuery = LogSanitizer.sanitizeQueryString(queryStr);
            String sanitizedUri = StrUtil.isBlank(sanitizedQuery) ? uri : uri + "?" + sanitizedQuery;
            String logMessage = StrUtil.format("[{}][{}-{}:{}ms] {} [{}]",
                    ip, method, status, DateUtil.current() - start, sanitizedUri,
                    JSONUtil.toJsonStr(LogSanitizer.sanitizeHeadersForLog(cachedHeader)));
            if (status >= 200 && status < 300) {
                log.info(logMessage);
            } else if (status >= 300 && status < 400) {
                log.warn(logMessage);
            } else {
                log.error(logMessage);
            }
        });
    }
}
