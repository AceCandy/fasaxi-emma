package cn.acecandy.fasaxi.emma.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * tmdb配置类
 *
 * @author tangningzhu
 * @since 2024/11/25
 */
@Data
@ConfigurationProperties(prefix = "tmdb")
public class TmdbConfig {
    /**
     * tmdb地址
     */
    private String host;
    /**
     * api key
     */
    private String apiKey;

    /**
     * 图片信息url
     */
    private String imageInfoUrl;

    /**
     * 图片静态url
     */
    private String imageStaticUrl;

    /**
     * 图片cdn静态url
     */
    private String imageCdnUrl;
}