package cn.acecandy.fasaxi.emma.utils;


import org.dromara.hutool.core.regex.PatternPool;

import java.util.regex.Pattern;

/**
 * emby 工具类
 *
 * @author tangningzhu
 * @since 2024/10/16
 */
public final class ReUtil extends org.dromara.hutool.core.regex.ReUtil {
    private ReUtil() {
    }

    /**
     * 匹配类似 /emby/Users/656fcefa283149708880b416786e5fde/Items/1417552
     */
    private static final Pattern REGEX_ITEM
            = PatternPool.get("^/emby/Users/[a-zA-Z0-9]+/Items/[0-9]+$", Pattern.DOTALL);
    private static final Pattern REGEX_ITEM_ID = PatternPool.get("/Items/([0-9]+)$", Pattern.DOTALL);

    public static boolean isItemUrl(String url) {
        return isMatch(REGEX_ITEM, url);
    }

    public static String getItemIdByUrl(String url) {
        return getGroup1(REGEX_ITEM_ID, url);
    }

    public static void main(String[] args) {
        String url = "/emby/Users/656fcefa283149708880b416786e5fde/Items";
        System.out.println(isItemUrl(url));
        System.out.println(getItemIdByUrl(url));
    }
}