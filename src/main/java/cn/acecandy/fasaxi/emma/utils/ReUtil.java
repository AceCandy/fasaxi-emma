package cn.acecandy.fasaxi.emma.utils;


import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.core.lang.Console;
import cn.hutool.v7.core.lang.mutable.MutablePair;
import cn.hutool.v7.core.math.NumberUtil;
import cn.hutool.v7.core.regex.PatternPool;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.core.text.split.SplitUtil;

import java.util.List;
import java.util.regex.Pattern;

/**
 * emby 工具类
 *
 * @author tangningzhu
 * @since 2024/10/16
 */
public final class ReUtil extends cn.hutool.v7.core.regex.ReUtil {
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
    // public static final Pattern REGEX_DOUBAN_JSON_ID = PatternPool.get(
    //         "movie\\\\/subject\\\\/(\\d+)", Pattern.DOTALL);
    public static final Pattern REGEX_DOUBAN_JSON_ID = PatternPool.get(
            "(/movie/|/subject/)(\\d+)", Pattern.DOTALL);

    public static final Pattern YEAR_PATTERN =
            PatternPool.get("\\b(19\\d{2}|20\\d{2})\\b", Pattern.DOTALL);
    /**
     * "爱奇艺第一季",
     * "爱奇艺第2季",
     * "爱奇艺 第三季",
     * "爱奇艺 第4季",
     * "爱奇艺5",
     * "爱奇艺2025",
     * "喜人奇妙夜第一季",
     * "天地剑心第2季",
     * "欢乐喜剧人 第10季",
     * "脱口秀大会5",
     * "跨年晚会2024"
     * <p>
     * 提取中间的名字和季号
     */
    public static final Pattern REGEX_TV_SEASON = PatternPool.get(
            "(.+?)(?:\\s*第?)?\\s*([\\d一二三四五六七八九十百千万廿卅〇]+)(?:季)?$", Pattern.DOTALL);
    // "(.+?)(?:\\s*第?)\\s*(\\d+)(?:季)?", Pattern.DOTALL);
    /**
     * 匹配类似 /emby/Users/656fcefa283149708880b416786e5fde/Items/1417552/Delete
     */
    private static final Pattern REGEX_SIMILAR_ITEM = PatternPool.get(
            "(/emby)?/users/([a-z0-9]+)/(?:[a-z]*items|items)/(-?[0-9]+)(/delete)?$", Pattern.DOTALL);
    /**
     * 匹配类似 /emby/Users/656fcefa283149708880b416786e5fde/Items/1417552
     */
    private static final Pattern REGEX_ITEM = PatternPool.get(
            "(/emby)?/users/([a-z0-9]+)/items/([0-9]+)$", Pattern.DOTALL);
    private static final Pattern REGEX_VIEW = PatternPool.get(
            "(/emby)?/users/([a-z0-9]+)/views$", Pattern.DOTALL);
    private static final Pattern REGEX_LATEST = PatternPool.get(
            "(/emby)?/users/([a-z0-9]+)/items/latest$", Pattern.DOTALL);
    private static final Pattern REGEX_RESUME = PatternPool.get(
            "(/emby)?/users/([a-z0-9]+)/items/resume$", Pattern.DOTALL);
    private static final Pattern REGEX_ITEMS = PatternPool.get(
            "(/emby)?/users/([a-z0-9]+)/items$", Pattern.DOTALL);
    private static final Pattern REGEX_COMMON_USER = PatternPool.get(
            "(/emby)?/users/([a-z0-9]+)/items", Pattern.DOTALL);
    private static final Pattern REGEX_PLAYLIST = PatternPool.get(
            "(/emby)?/playlist/(\\d+)/items$", Pattern.DOTALL);
    private static final Pattern REGEX_PLAYBACK = PatternPool.get(
            "(/emby)?/items/(\\d+)/playbackinfo$", Pattern.DOTALL);
    private static final Pattern REGEX_VIDEO = PatternPool.get(
            "(/emby)?/(videos|items)/(\\d+)/(?:original\\.[^/]+|download|stream.*)$", Pattern.DOTALL);
    private static final Pattern REGEX_AUDIO = PatternPool.get(
            "(/emby)?/audio/(\\d+)/(?:original\\.[^/]+|download|stream|stream.*)$", Pattern.DOTALL);

    private ReUtil() {
    }

    /**
     * 解析猫眼电视剧名称季
     *
     * @param originName 原产地名称
     * @return {@link MutablePair }<{@link String }, {@link String }>
     */
    public static MutablePair<String, Integer> parseMaoyanTvNameSeason(String originName) {
        originName = StrUtil.trim(originName);
        if (StrUtil.isBlank(originName)) {
            return MutablePair.of(null, null);
        }
        List<String> groups = getAllGroups(REGEX_TV_SEASON, originName);
        if (CollUtil.isEmpty(groups) || groups.size() < 3) {
            return MutablePair.of(originName, null);
        }
        String name = CollUtil.get(groups, 1);
        String seasonPort = StrUtil.removeAll(
                StrUtil.removePrefix(originName, name), "第", "季").trim();
        int seasonNumber = 0;
        if (PinYinUtil.isChinese(seasonPort)) {
            seasonNumber = NumberUtil.parseInt(seasonPort);
        }
        return MutablePair.of(name, seasonNumber);
    }

    public static String findDouBanIdByHtml(String html) {
        html = StrUtil.trim(html);
        if (StrUtil.isBlank(html)) {
            return null;
        }
        return ReUtil.getGroup1(REGEX_DOUBAN_HTML_ID, html);
    }

    public static String parseYearByHtml(String html) {
        html = StrUtil.trim(html);
        if (StrUtil.isBlank(html)) {
            return null;
        }
        return ReUtil.getGroup1(YEAR_PATTERN, html);
    }

    public static String findDouBanIdByJson(String json) {
        json = StrUtil.trim(json);
        if (StrUtil.isBlank(json)) {
            return null;
        }
        return ReUtil.get(REGEX_DOUBAN_JSON_ID, json, 2);
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

    public static String isViewUrl(String url) {
        List<String> groups = getAllGroups(REGEX_VIEW, url.toLowerCase());
        if (groups == null || groups.size() < 3) {
            return null;
        }
        return CollUtil.get(groups, 2);
    }

    public static String isLatestUrl(String url) {
        List<String> groups = getAllGroups(REGEX_LATEST, url.toLowerCase());
        if (groups == null || groups.size() < 3) {
            return null;
        }
        return CollUtil.get(groups, 2);
    }

    public static String isResumeUrl(String url) {
        List<String> groups = getAllGroups(REGEX_RESUME, url.toLowerCase());
        if (groups == null || groups.size() < 3) {
            return null;
        }
        return CollUtil.get(groups, 2);
    }

    public static String isCommonUserUrl(String url) {
        List<String> groups = getAllGroups(REGEX_COMMON_USER, url.toLowerCase());
        if (groups == null || groups.size() < 3) {
            return null;
        }
        return CollUtil.get(groups, 2);
    }

    public static boolean isItemsUrl(String url) {
        return isMatch(REGEX_ITEMS, url.toLowerCase());
    }

    public static String getUserByItemsUrl(String url) {
        List<String> groups = getAllGroups(REGEX_ITEMS, url.toLowerCase());
        if (groups == null || groups.size() < 3) {
            return null;
        }
        return CollUtil.get(groups, 2);
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
        Console.log(parseMaoyanTvNameSeason("我不是药神2023"));
        Console.log(parseMaoyanTvNameSeason("我不是药神第六季"));
        Console.log(parseMaoyanTvNameSeason("我不是药神3"));
        Console.log(parseMaoyanTvNameSeason("我不是药神"));
        Console.log(parseMaoyanTvNameSeason("我不是药神第2季"));
        Console.log(parseMaoyanTvNameSeason("我不是药神 第一季"));
        Console.log(parseMaoyanTvNameSeason("我不是药神 第4季"));
    }
}