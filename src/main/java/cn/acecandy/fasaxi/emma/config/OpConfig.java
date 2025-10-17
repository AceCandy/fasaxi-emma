package cn.acecandy.fasaxi.emma.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * openlist配置类
 *
 * @author tangningzhu
 * @since 2024/11/25
 */
@Data
@ConfigurationProperties(prefix = "openlist")
public class OpConfig {
    /**
     * 路径
     */
    private String host;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

}