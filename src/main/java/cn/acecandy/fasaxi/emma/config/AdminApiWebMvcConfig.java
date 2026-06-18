package cn.acecandy.fasaxi.emma.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 管理接口 MVC 配置
 *
 * @author Codex
 * @since 2026/06/10
 */
@Configuration
@RequiredArgsConstructor
public class AdminApiWebMvcConfig implements WebMvcConfigurer {

    private final AdminApiInterceptor adminApiInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminApiInterceptor)
                .addPathPatterns(
                        "/api/debug/**",
                        "/api/build/**",
                        "/api/clear/**",
                        "/api/obtainCollection"
                );
    }
}
