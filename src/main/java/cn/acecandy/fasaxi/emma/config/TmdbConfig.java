package cn.acecandy.fasaxi.emma.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

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
     * 详情信息url
     */
    private String detailInfoUrl;

    /**
     * 搜索详情信息url
     */
    private String searchDetailInfoUrl;

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
    private List<String> imageCdnUrl;
}