package cn.acecandy.fasaxi.emma.utils;


import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.collection.ListUtil;
import org.dromara.hutool.core.lang.Console;
import org.dromara.hutool.core.regex.PatternPool;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.core.text.split.SplitUtil;

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
     * 匹配标题中的标点符号进行分割
     */
    public static final Pattern REGEX_DOUBAN_HTML_ID = PatternPool.get(
            "subject_id:\\s*'([0-9]+)'", Pattern.DOTALL);

    /**
     * 匹配标题中的标点符号进行分割
     */
    public static final Pattern REGEX_DOUBAN_JSON_ID = PatternPool.get(
            "movie\\\\/subject\\\\/(\\d+)", Pattern.DOTALL);

    /**
     * 匹配类似 /emby/Users/656fcefa283149708880b416786e5fde/Items/1417552/Delete
     */
    private static final Pattern REGEX_SIMILAR_ITEM = PatternPool.get(
            "(/emby)?/users/([a-z0-9]+)/(?:[a-z]*items|items)/([0-9]+)(/delete)?$", Pattern.DOTALL);

    /**
     * 匹配类似 /emby/Users/656fcefa283149708880b416786e5fde/Items/1417552
     */
    private static final Pattern REGEX_ITEM = PatternPool.get(
            "(/emby)?/users/([a-z0-9]+)/items/([0-9]+)$", Pattern.DOTALL);

    private static final Pattern REGEX_ITEMS = PatternPool.get(
            "(/emby)?/users/([a-z0-9]+)/items$", Pattern.DOTALL);

    private static final Pattern REGEX_PLAYLIST = PatternPool.get(
            "(/emby)?/playlist/(\\d+)/items$", Pattern.DOTALL);

    private static final Pattern REGEX_PLAYBACK = PatternPool.get(
            "(/emby)?/items/(\\d+)/playbackinfo$", Pattern.DOTALL);

    private static final Pattern REGEX_VIDEO = PatternPool.get(
            "(/emby)?/(videos|items)/(\\d+)/(?:original\\.[^/]+|download|stream.*)$", Pattern.DOTALL);

    private static final Pattern REGEX_AUDIO = PatternPool.get(
            "(/emby)?/audio/(\\d+)/(?:original\\.[^/]+|download|stream|stream.*)$", Pattern.DOTALL);

    public static String findDouBanIdByHtml(String html) {
        html = StrUtil.trim(html);
        if (StrUtil.isBlank(html)) {
            return null;
        }
        return ReUtil.getGroup1(REGEX_DOUBAN_HTML_ID, html);
    }

    public static String findDouBanIdByJson(String json) {
        json = StrUtil.trim(json);
        if (StrUtil.isBlank(json)) {
            return null;
        }
        return ReUtil.getGroup1(REGEX_DOUBAN_JSON_ID, json);
    }

    public static String isPlaybackUrl(String url) {
        List<String> groups = getAllGroups(REGEX_PLAYBACK, url.toLowerCase());
        if (CollUtil.isEmpty(groups) || groups.size() < 3) {
            return null;
        }
        return CollUtil.get(groups, 2);
    }

    public static String isPlaylistUrl(String url) {
        List<String> groups = getAllGroups(REGEX_PLAYLIST, url.toLowerCase());
        if (CollUtil.isEmpty(groups) || groups.size() < 3) {
            return null;
        }
        return CollUtil.get(groups, 2);
    }

    public static String isVideoUrl(String url) {
        List<String> groups = getAllGroups(REGEX_VIDEO, url.toLowerCase());
        if (CollUtil.isEmpty(groups) || groups.size() < 4) {
            return null;
        }
        return CollUtil.get(groups, 3);
    }

    public static String isAudioUrl(String url) {
        List<String> groups = getAllGroups(REGEX_AUDIO, url.toLowerCase());
        if (groups == null || groups.size() < 3) {
            return null;
        }
        return CollUtil.get(groups, 2);
    }

    public static List<String> isSimilarItemUrl(String url) {
        List<String> groups = getAllGroups(REGEX_SIMILAR_ITEM, url.toLowerCase());
        if (groups == null || groups.size() < 2) {
            return null;
        }
        return ListUtil.of(CollUtil.get(groups, 2), CollUtil.get(groups, 3));
    }

    public static List<String> isItemUrl(String url) {
        List<String> groups = getAllGroups(REGEX_ITEM, url.toLowerCase());
        if (groups == null || groups.size() < 3) {
            return null;
        }
        return ListUtil.of(CollUtil.get(groups, 2), CollUtil.get(groups, 3));
    }

    public static boolean isItemsUrl(String url) {
        return isMatch(REGEX_ITEMS, url.toLowerCase());
    }


    /**
     * 将连接符号两边的单词首字母转为大写
     *
     * @param input 输入字符串
     * @return 转换后的字符串
     */
    public static String capitalizeWords(String input) {
        if (StrUtil.isBlank(input)) {
            return input;
        }
        // 使用第一个分隔符分割字符串
        List<String> parts = SplitUtil.split(input, "-");

        // 首字母大写后重新拼接
        return StrUtil.join("-", parts.stream().map(StrUtil::upperFirst).toList());
    }

    public static void main(String[] args) {
        // String url = "/items/123123/playbackinfo".toLowerCase();
        String url = "/emby/Users/656fcefa283149708880b416786e5fde/Items/1417552/Delete".toLowerCase();
        Console.log(isSimilarItemUrl(url));
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