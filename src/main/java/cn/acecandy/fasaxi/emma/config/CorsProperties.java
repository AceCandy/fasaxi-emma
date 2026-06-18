package cn.acecandy.fasaxi.emma.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 跨域配置属性
 *
 * @author Codex
 * @since 2026/06/10
 */
@Data
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

    /**
     * 允许跨域的来源，默认不放开。
     */
    private List<String> allowedOriginPatterns = new ArrayList<>();

    /**
     * 允许的请求方法。
     */
    private List<String> allowedMethods = new ArrayList<>(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

    /**
     * 允许的请求头。
     */
    private List<String> allowedHeaders = new ArrayList<>(List.of("*"));

    /**
     * 是否允许携带凭证。
     */
    private boolean allowCredentials;

    /**
     * 预检请求缓存时间。
     */
    private long maxAge = 3600;
}
