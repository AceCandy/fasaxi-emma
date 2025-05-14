package cn.acecandy.fasaxi.emma.utils;


import cn.acecandy.fasaxi.emma.common.enums.EmbyPicType;
import cn.acecandy.fasaxi.emma.common.ex.BaseException;
import cn.acecandy.fasaxi.emma.config.TmdbConfig;
import cn.acecandy.fasaxi.emma.dao.entity.EmbyItemPic;
import jakarta.servlet.http.HttpServletRequest;
import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.math.NumberUtil;
import org.dromara.hutool.core.net.url.UrlDecoder;
import org.dromara.hutool.core.net.url.UrlUtil;
import org.dromara.hutool.core.regex.ReUtil;
import org.dromara.hutool.core.regex.RegexPool;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.core.text.split.SplitUtil;

import java.nio.charset.Charset;
import java.util.List;

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
        int index = 0;
        // int index = (path.hashCode() & Integer.MAX_VALUE) % 2;
        return StrUtil.format(CollUtil.get(tmdbConfig.getImageCdnUrl(), index), maxWidth) + path;
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

    public static boolean isHttpOk(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * 构建香港url
     *
     * @param mediaPath 媒体路径
     * @return {@link String }
     */
    public static String getPtUrlOnHk(String mediaPath) {
        if (StrUtil.isBlank(mediaPath)) {
            return "";
        }
        mediaPath = UrlUtil.normalize(UrlDecoder.decode(mediaPath, Charset.defaultCharset()));
        mediaPath = StrUtil.replace(mediaPath, "https://alist.acecandy.cn:880/d/pt/Emby1/",
                "http://8.210.221.216:5244/p/bt/Emby1/");
        mediaPath = StrUtil.replace(mediaPath, "https://alist.acecandy.cn:880/d/pt/Emby/",
                "http://8.210.221.216:5244/p/pt/Emby/");
        return mediaPath;
    }

    public static Range parseRangeHeader(String range, Long totalSize) {
        long chunk = 10 * 1024 * 1024;
        if (null == totalSize || NumberUtil.isZero(totalSize)) {
            totalSize = Long.MAX_VALUE;
        }
        if (StrUtil.isBlank(range) || !StrUtil.startWith(range, "bytes=")) {
            return new Range(0, Math.min(chunk, totalSize) - 1, totalSize);
        }
        range = StrUtil.removePrefixIgnoreCase(range, "bytes=");
        List<String> parts = SplitUtil.split(range, "-");
        long start = NumberUtil.parseLong(CollUtil.getFirst(parts));
        if (start > totalSize - 1) {
            return null;
        }
        if (start + chunk > totalSize - 1) {
            start = totalSize - chunk;
        }
        // Long end = NumberUtil.parseLong(CollUtil.getLast(parts));
        return new Range(start, Math.min(chunk + start, totalSize - 1), totalSize);
    }

    /**
     * 请求头 range范围
     *
     * @author AceCandy
     * @since 2025/05/12
     */
    public record Range(long start, long end, long size) {
        // 写一个方法输出bytes=start-end的字符串
        public String toHeader() {
            return StrUtil.format("bytes={}-{}", start, end);
        }
    }
}