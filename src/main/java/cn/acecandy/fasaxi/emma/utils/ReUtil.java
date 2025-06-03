package cn.acecandy.fasaxi.emma.utils;


import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.collection.ListUtil;
import org.dromara.hutool.core.regex.PatternPool;

import java.util.List;
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
     * 匹配标题中的标点符号进行分割
     */
    public static final Pattern REGEX_SPILT_TITLE = PatternPool.get(
            "[,.，。；;'\"“”‘’：:【】]", Pattern.DOTALL);

    /**
     * 匹配类似 /emby/Users/656fcefa283149708880b416786e5fde/Items/1417552/Delete
     */
    private static final Pattern REGEX_SIMILAR_ITEM = PatternPool.get(
            "^/emby/users/([a-z0-9]+)/[a-z]*items/([0-9]+)(/delete)?$", Pattern.DOTALL);

    /**
     * 匹配类似 /emby/Users/656fcefa283149708880b416786e5fde/Items/1417552
     */
    private static final Pattern REGEX_ITEM = PatternPool.get(
            "^/emby/users/([a-z0-9]+)/items/([0-9]+)$", Pattern.DOTALL);

    private static final Pattern REGEX_ITEMS = PatternPool.get(
            "^/emby/users/([a-z0-9]+)/items$", Pattern.DOTALL);

    private static final Pattern REGEX_PLAYLIST = PatternPool.get(
            "^/playlist/(\\d+)/items$", Pattern.DOTALL);

    private static final Pattern REGEX_VIDEO = PatternPool.get(
            "^/emby/(videos|items)/(\\d+)/(?:original\\.[^/]+|download|stream.*)$", Pattern.DOTALL);

    private static final Pattern REGEX_AUDIO = PatternPool.get(
            "^/audio/(\\d+)/(?:original\\.[^/]+|download|stream|stream.*)$", Pattern.DOTALL);

    public static String isPlaylistUrl(String url) {
        List<String> groups = getAllGroups(REGEX_PLAYLIST, url.toLowerCase());
        if (CollUtil.isEmpty(groups)) {
            return null;
        }
        return CollUtil.get(groups, 0);
    }

    public static String isVideoUrl(String url) {
        List<String> groups = getAllGroups(REGEX_VIDEO, url.toLowerCase());
        if (CollUtil.isEmpty(groups) || groups.size() < 2) {
            return null;
        }
        return CollUtil.get(groups, 2);
    }

    public static String isAudioUrl(String url) {
        List<String> groups = getAllGroups(REGEX_AUDIO, url.toLowerCase());
        if (groups == null || groups.size() < 2) {
            return null;
        }
        return CollUtil.get(groups, 1);
    }

    public static List<String> isSimilarItemUrl(String url) {
        List<String> groups = getAllGroups(REGEX_SIMILAR_ITEM, url.toLowerCase());
        if (groups == null || groups.size() < 2) {
            return null;
        }
        return ListUtil.of(CollUtil.get(groups, 1), CollUtil.get(groups, 2));
    }

    public static List<String> isItemUrl(String url) {
        List<String> groups = getAllGroups(REGEX_ITEM, url.toLowerCase());
        if (groups == null || groups.size() < 2) {
            return null;
        }
        return ListUtil.of(CollUtil.get(groups, 1), CollUtil.get(groups, 2));
    }

    public static boolean isItemsUrl(String url) {
        return isMatch(REGEX_ITEMS, url.toLowerCase());
    }

    public static void main(String[] args) {
        String url = "/emby/Users/656fcefa283149708880b416786e5fde/Items/213123".toLowerCase();
        /*Console.log(url);
        System.out.println(isSimilarItemUrl(url));
        System.out.println(isItemUrl(url));
        url = "/emby/Users/656fcefa283149708880b416786e5fde/Items/213123/delete".toLowerCase();
        System.out.println(isSimilarItemUrl(url));
        System.out.println(isItemUrl(url));
        url = "/emby/Users/656fcefa283149708880b416786e5fde/Items".toLowerCase();
        System.out.println(isSimilarItemUrl(url));
        System.out.println(isItemUrl(url));
        url = "/emby/Users/656fcefa283149708880b416786e5fde".toLowerCase();
        System.out.println(isSimilarItemUrl(url));
        System.out.println(isItemUrl(url));*/
    }
}