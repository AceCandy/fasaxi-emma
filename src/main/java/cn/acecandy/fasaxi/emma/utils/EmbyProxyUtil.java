package cn.acecandy.fasaxi.emma.utils;


import cn.acecandy.fasaxi.emma.common.enums.EmbyPicType;
import cn.acecandy.fasaxi.emma.common.ex.BaseException;
import cn.acecandy.fasaxi.emma.config.TmdbConfig;
import cn.acecandy.fasaxi.emma.dao.entity.EmbyItemPic;
import jakarta.servlet.http.HttpServletRequest;
import org.dromara.hutool.core.regex.ReUtil;
import org.dromara.hutool.core.regex.RegexPool;
import org.dromara.hutool.core.text.StrUtil;

import static cn.acecandy.fasaxi.emma.common.enums.EmbyPicType.Logo;
import static cn.acecandy.fasaxi.emma.common.enums.EmbyPicType.封面;
import static cn.acecandy.fasaxi.emma.common.enums.EmbyPicType.背景图;
import static cn.acecandy.fasaxi.emma.common.enums.EmbyPicType.非图片;

/**
 * emby代理 工具类
 *
 * @author tangningzhu
 * @since 2024/10/16
 */
public final class EmbyProxyUtil {
    private EmbyProxyUtil() {
    }


    /**
     * 需要图片重定向
     *
     * @param req 绿色
     * @return {@link EmbyPicType }
     */
    public static EmbyPicType getPicType(HttpServletRequest req) {
        EmbyPicType type = 非图片;
        String url = req.getRequestURI().toLowerCase();
        if (!StrUtil.contains(url, "/items/")) {
            return type;
        }
        if (StrUtil.contains(url, "/images/primary")) {
            type = 封面;
        } else if (StrUtil.contains(url, "/images/backdrop")) {
            type = 背景图;
        } else if (StrUtil.contains(url, "/images/logo")) {
            type = Logo;
        }
        return type;
    }

    /**
     * 需要视频重定向
     *
     * @param req 绿色
     * @return boolean
     */
    public static boolean needVideoRedirect(HttpServletRequest req) {
        return StrUtil.containsAll(req.getRequestURI().toLowerCase(), "/emby/videos/", "/original");
    }

    /**
     * 需要屏蔽
     *
     * @param req 绿色
     * @return boolean
     */
    public static boolean needClose(HttpServletRequest req) {
        return StrUtil.contains(req.getRequestURI().toLowerCase(), "/embywebsocket");
    }

    /**
     * 从url中解析项目id
     *
     * @param url 网址
     * @return {@link String }
     */
    public static String parseItemIdByUrl(String url) {
        if (StrUtil.isBlank(url)) {
            return null;
        }
        return ReUtil.get(RegexPool.NUMBERS, url, 0);
    }

    /**
     * 获取静态图片地址
     *
     * @param path       后缀路径(无host)
     * @param tmdbConfig tmdb配置
     * @return {@link String }
     */
    public static String getCdnPicUrl(String path, TmdbConfig tmdbConfig, String maxWidth) {
        if (StrUtil.isBlank(path) || StrUtil.startWith(path, "http")) {
            return null;
        }
        if (StrUtil.isBlank(maxWidth)) {
            maxWidth = "400";
        }
        return StrUtil.format(tmdbConfig.getImageCdnUrl(), maxWidth) + path;
    }

    /**
     * 获取图片uri（无host）
     *
     * @param path       路径
     * @param tmdbConfig tmdb 配置
     * @return {@link String }
     */
    public static String getPicUri(String path, TmdbConfig tmdbConfig) {
        if (StrUtil.isBlank(path)) {
            return null;
        }
        return StrUtil.removePrefixIgnoreCase(path, tmdbConfig.getImageStaticUrl());
    }


    /**
     * 获取图片302 uri(无host)
     *
     * @param itemPic 项目图片
     * @param picType 图片类型
     * @return {@link String }
     */
    public static String getPic302Uri(EmbyItemPic itemPic, EmbyPicType picType) {
        if (null == itemPic) {
            return null;
        }
        return switch (picType) {
            case 封面 -> itemPic.getPosterPath();
            case 背景图 -> itemPic.getBackdropPath();
            case Logo -> itemPic.getLogoPath();
            default -> throw new BaseException("图片类型异常: " + picType);
        };
    }

    /**
     * 需要缓存的请求
     *
     * @param req 请求入参
     * @return boolean
     */
    public static boolean isCacheStaticReq(HttpServletRequest req) {
        String uri = req.getRequestURI().toLowerCase();
        if (StrUtil.containsAny(uri, "/images/primary", "/images/backdrop", "/images/logo")) {
            return true;
        }
        return uri.matches(".*\\.(js|css|woff2|png|jpg|gif|ico|json|html)$");
    }

    /**
     * 通过容器获取内容类型
     *
     * @param container 容器
     * @return {@link String }
     */
    public static String getContentType(String container) {
        return switch (container) {
            case "mp4" -> "video/mp4";
            case "mkv" -> "video/x-matroska";
            case "webm" -> "video/webm";
            case "ogg" -> "video/ogg";
            case "mpeg", "mpg" -> "video/mpeg";
            case "mov" -> "video/quicktime";
            case "ts" -> "video/mp2t";
            case "wmv" -> "video/x-ms-wmv";
            case "avi" -> "video/x-msvideo";
            case "flv" -> "video/x-flv";
            default -> "application/octet-stream";
        };
    }
}