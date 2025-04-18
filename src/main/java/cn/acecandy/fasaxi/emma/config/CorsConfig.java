package cn.acecandy.fasaxi.emma.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置
 *
 * @author tangningzhu
 * @since 2025/4/16
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")  // 允许所有来源
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // 包含OPTIONS方法
                .allowedHeaders("*")
                .allowCredentials(true)       // 允许携带凭证（如Cookie）
                .maxAge(3600);                // 预检请求缓存时间
    }
}