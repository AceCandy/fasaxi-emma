package cn.acecandy.fasaxi.emma.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * douban配置类
 *
 * @author tangningzhu
 * @since 2024/11/25
 */
@Data
@ConfigurationProperties(prefix = "douban")
public class DoubanConfig {
    /**
     * 图片静态url
     */
    private String imageStaticUrl;
    /**
     * 图片cdn url
     */
    private List<String> imageCdnUrl;
    /**
     * app相关
     */
    private HostInfo app;
    /**
     * 小程序相关
     */
    private HostInfo wxmini;
    /**
     * 小程序相关
     */
    private DouBanApi localApi;

    @Data
    public static class HostInfo {
        /**
         * 域名
         */
        private String host;
        /**
         * apikey
         */
        private List<String> apikey;
        /**
         * 详情信息url
         */
        private String detailInfoUrl;
    }

    @Data
    public static class DouBanApi {
        /**
         * 域名
         */
        private String host;
        /**
         * 搜索imdb url
         */
        private String searchImdbUrl;
        /**
         * 详情信息url
         */
        private String doubanItemUrl;
    }
}