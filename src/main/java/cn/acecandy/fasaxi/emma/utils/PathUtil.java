package cn.acecandy.fasaxi.emma.utils;

import cn.hutool.v7.core.lang.Console;
import cn.hutool.v7.core.text.StrUtil;
import lombok.extern.slf4j.Slf4j;


/**
 * 路径工具类
 *
 * @author tangningzhu
 * @since 2023/12/20
 */
@Slf4j
public final class PathUtil extends cn.hutool.v7.core.io.file.PathUtil {

    private PathUtil() {
    }

    /**
     * 替换url路径(比如把包含/d/pt的域名部分 替换为新的域名路径)
     *
     * @param url           网址
     * @param pattern       模式 "/d/pt"
     * @param targetBaseUrl 目标基本url
     * @return {@link String }
     */
    public static String replaceAfterUrlPath(String url, String pattern, String targetBaseUrl) {
        if (StrUtil.contains(url, pattern)) {
            // 截取pattern后面的部分（包含斜杠），如果不存在则返回空串
            String suffix = StrUtil.subAfter(url, pattern, false);
            // 拼接新URL（Hutool的StrUtil.concat避免空指针）
            return StrUtil.concat(true, targetBaseUrl, suffix);
        }
        return url;
    }

    public static void main(String[] args) {
        Console.log(replaceAfterUrlPath("https://alist.acecandy.cn:880/d/pt/emby1/国产剧/大明王朝/S01/E03.mkv", "/d/pt", "https://168.1.1.1:990/pt"));
    }
}