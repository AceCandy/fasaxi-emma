package cn.acecandy.fasaxi.emma.sao.config;

import cn.acecandy.fasaxi.emma.utils.HtmlUtil;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

/**
 * 添加随机UA
 *
 * @author tangningzhu
 * @since 2025/8/18
 */
public class RandomUaFeignConfig {

    @Bean
    public RequestInterceptor randomUserAgentInterceptor() {
        return template -> {
            // 如果调用方已经手动传了 UA，就别覆盖（按需）
            if (template.headers().containsKey("User-Agent")) {
                return;
            }
            // 防止重复叠加（有些情况下 template.header 会追加多个值）
            template.headers().remove("User-Agent");
            template.header("User-Agent", HtmlUtil.randomUserAgent());
        };
    }
}