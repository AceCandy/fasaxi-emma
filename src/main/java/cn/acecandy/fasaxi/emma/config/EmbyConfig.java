package cn.acecandy.fasaxi.emma.config;

import lombok.Data;
import org.dromara.hutool.core.lang.Console;
import org.dromara.hutool.core.regex.ReUtil;
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
     * 内网emby ws地址
     */
    private String wsHost;
    /**
     * apiKey
     */
    private String apiKey;
    /**
     * 通用ua
     */
    private String commonUa;
    /**
     * 项目信息url
     */
    private String itemInfoUrl;
    /**
     * 媒体信息url
     */
    private String playbackUrl;
    /**
     * 剧集信息url
     */
    private String episodesUrl;
    /**
     * 获取远程图片url
     */
    private String remoteImagesUrl;
    /**
     * 刷新tmdb url
     */
    private String refreshUrl;
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

    public static void main(String[] args) {
        String s = "^/emby/Users/[a-zA-Z0-9]+/Items/[0-9]+$";
        String s1 = "/Items/([0-9]+)$";
        String url = "/emby/Users/656fcefa283149708880b416786e5fde/Items/1417552";
        String url1 = "/emby/Users1/656fcefa283149708880b416786e5fde/Items/1417552";
        String url2 = "/emby/Users1/656fcefa283149708880b416786e5fde/Items/1417552/123";
        Console.log(ReUtil.isMatch(s,url));
        Console.log(ReUtil.getGroup1(s1,url));
        Console.log(ReUtil.isMatch(s,url1));
        Console.log(ReUtil.isMatch(s,url2));
    }
}