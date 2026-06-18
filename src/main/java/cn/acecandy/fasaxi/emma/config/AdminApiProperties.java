package cn.acecandy.fasaxi.emma.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理接口访问控制配置
 *
 * @author Codex
 * @since 2026/06/10
 */
@Data
@ConfigurationProperties(prefix = "admin-api")
public class AdminApiProperties {

    /**
     * 管理接口总开关，默认关闭。
     */
    private boolean enabled;

    /**
     * 访问管理接口所需的令牌。
     */
    private String token;

    /**
     * 允许启用管理接口的 profile，默认仅 local。
     */
    private List<String> allowedProfiles = new ArrayList<>(List.of("local"));

    /**
     * 自定义管理令牌请求头名称。
     */
    private String headerName = "X-Admin-Token";
}
