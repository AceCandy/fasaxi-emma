package cn.acecandy.fasaxi.emma.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * maoyan配置类
 *
 * @author tangningzhu
 * @since 2024/11/25
 */
@Data
@ConfigurationProperties(prefix = "maoyan")
public class MaoyanConfig {
    /**
     * 主机地址
     */
    private String host;
    /**
     * 获取电影信息url
     */
    private String movieUrl;
    /**
     * 获取电视剧信息url
     */
    private String tvUrl;

}