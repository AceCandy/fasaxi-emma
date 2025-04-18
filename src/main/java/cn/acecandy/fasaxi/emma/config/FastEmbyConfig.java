package cn.acecandy.fasaxi.emma.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * @author tangningzhu
 * @since 2024/11/25
 */
// @Configuration
@Data
@ConfigurationProperties(prefix = "emby")
public class FastEmbyConfig {
    /**
     * 内网emby地址
     */
    private String host;

    /**
     * 公网emby地址
     */
    private String publicAddr;

    /**
     * 公共alist地址
     */
    private String alistPublic;

    /**
     * 内部alist地址
     */
    private String alistInner;
    /**
     * 原始pt路径
     */
    private String originPt;
    /**
     * 转发pt路径1
     */
    private String transPt1;
    /**
     * 转发pt路径2
     */
    private String transPt2;
    /**
     * 转发pt路径3
     */
    private String transPt3;
    /**
     * 转发pt路径4
     */
    private String transPt4;

    /**
     * strm相关路径
     */
    private List<String> strmPaths;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}