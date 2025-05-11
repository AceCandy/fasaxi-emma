package cn.acecandy.fasaxi.emma.config;

import cn.acecandy.fasaxi.emma.utils.ThreadUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.date.DateUtil;
import org.dromara.hutool.core.text.StrUtil;
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

    @Resource
    private EmbyConfig embyConfig;

    public void log(String method, String uri, String ip, String queryStr, int status, long start) {
        ThreadUtil.execVirtual(() -> {
            String logMessage = StrUtil.format("[{}][{}-{}:{}ms] {}?{}&api_key={}", ip, method, status,
                    DateUtil.current() - start, uri, queryStr, embyConfig.getApiKey());
            if (status >= CODE_200 && status < CODE_300) {
                log.info(logMessage);
            } else if (status >= CODE_300 && status < CODE_400) {
                log.warn(logMessage);
            } else {
                log.error(logMessage);
            }
        });
    }
}