package cn.acecandy.fasaxi.emma.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author tangningzhu
 * @since 2024/11/25
 */
// @Configuration
@Data
@ConfigurationProperties(prefix = "emby")
public class EmbyConfig {
    /**
     * 内网emby地址
     */
    private String host;
    /**
     * apiKey
     */
    private String apiKey;
    /**
     * 媒体信息url
     */
    private String itemInfoUrl;
    /**
     * 远程图片url
     */
    private String remoteImagesUrl;
    /**
     * 缓存路径
     */
    private String cachePath;

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
}