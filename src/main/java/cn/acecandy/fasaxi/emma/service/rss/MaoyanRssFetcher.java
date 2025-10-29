package cn.acecandy.fasaxi.emma.service.rss;

import cn.acecandy.fasaxi.emma.sao.out.RTmdbMovie;
import cn.acecandy.fasaxi.emma.sao.out.RTmdbTv;
import cn.acecandy.fasaxi.emma.sao.proxy.MaoyanProxy;
import cn.acecandy.fasaxi.emma.sao.proxy.TmdbProxy;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.core.collection.set.SetUtil;
import cn.hutool.v7.core.lang.mutable.MutablePair;
import cn.hutool.v7.core.map.MapUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.json.JSONArray;
import cn.hutool.v7.json.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static cn.acecandy.fasaxi.emma.sao.proxy.MaoyanProxy.PLATFORM_MAP;
import static cn.acecandy.fasaxi.emma.sao.proxy.MaoyanProxy.TV_HEAT_MAP;

/**
 *
 * @author tangningzhu
 * @since 2025/10/29
 */
@Slf4j
@Service
public class MaoyanRssFetcher {

    @Resource
    private MaoyanProxy maoyanProxy;

    @Resource
    private TmdbProxy tmdbProxy;


    private static final Pattern SEASON_PATTERN_CN = Pattern.compile("(.*?)\\s*[（(]?\\s*(第?[一二三四五六七八九十百]+)\\s*季\\s*[)）]?");
    private static final Pattern SEASON_PATTERN_EN = Pattern.compile("(.*?)\\s+Season\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SEASON_PATTERN_NUM = Pattern.compile("^(.*?)\\s*(\\d+)$");
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(19|20)\\d{2}$");

    private void exec(List<String> keys, String platform, Integer limit) {
        MutablePair<Set<String>, Set<String>> topTitles =
                getMaoyanTopTitles(keys, platform);

        List<MatchedItem> matchedMovies = matchTitlesToTmdb(
                topTitles.getLeft(), "Movie");

        List<MatchedItem> matchedSeries = matchTitlesToTmdb(
                topTitles.getRight(), "Series");

        List<MatchedItem> allItems = ListUtil.of();
        allItems.addAll(matchedMovies);
        allItems.addAll(matchedSeries);

        /*List<Map<String, String>> uniqueItems = allItems.stream()
                .collect(Collectors.toMap(
                        item -> item.get("type") + "-" + item.get("id"),
                        item -> item,
                        (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .collect(Collectors.toList());

        try (Writer writer = new FileWriter(argsParser.getOutputFile(), StandardCharsets.UTF_8)) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, uniqueItems);
            log.info("成功将 {} 个项目写入到缓存文件: {}", uniqueItems.size(), argsParser.getOutputFile());
        }*/

        log.info("任务执行完毕。");
    }

    /**
     * 匹配项目
     *
     * @author AceCandy
     * @since 2025/10/29
     */
    public record MatchedItem(Integer id, String name, String type) {
    }

    /**
     * 季信息
     *
     * @author AceCandy
     * @since 2025/10/29
     */
    public record SeriesInfo(String showName, Integer seasonNumber) {
    }

    /**
     * 获取猫眼榜单的所有电影+电视剧标题
     *
     * @param keys     钥匙
     * @param platform 平台
     * @return {@link MutablePair }<{@link Set }<{@link String }>, {@link Set }<{@link String }>>
     */
    private MutablePair<Set<String>, Set<String>> getMaoyanTopTitles(
            List<String> keys, String platform) {
        Set<String> moviesList = SetUtil.of();
        Set<String> tvTitles = SetUtil.of();

        if (keys.contains("movie")) {
            JSONArray movieTop = maoyanProxy.getMovieTop();
            movieTop.forEach(i -> {
                JSONObject movieItem = (JSONObject) i;
                moviesList.add(movieItem.getJSONObject("movieInfo").getStr("movieName"));
            });
        }

        List<String> tvTypesToFetch = keys.stream().filter(TV_HEAT_MAP::containsKey).toList();
        if (CollUtil.isNotEmpty(tvTypesToFetch)) {
            Integer platformCode = MapUtil.getInt(PLATFORM_MAP, platform, null);
            tvTypesToFetch.forEach(tvType -> {
                Integer seriesTypeCode = MapUtil.getInt(TV_HEAT_MAP, tvType);
                JSONArray tvTop = maoyanProxy.getTvTop(seriesTypeCode, platformCode);
                tvTop.forEach(i -> {
                    JSONObject tvItem = (JSONObject) i;
                    tvTitles.add(tvItem.getJSONObject("seriesInfo").getStr("name"));
                });
            });
        }
        return MutablePair.of(moviesList, tvTitles);
    }

    private static SeriesInfo parseSeriesTitle(String title) {
        if (StrUtil.isBlank(title)) {
            return new SeriesInfo(null, null);
        }
        String showName = title.strip();
        Integer seasonNumber = null;

        /*MutablePair<String, String> parsedResult = ReUtil.parseMaoyanTvNameSeason(showName);
        if (matchCn.find()) {
            showName = matchCn.group(1).strip();
            if (seasonNumber == null) {
                String seasonWord = matchCn.group(2);
                seasonNumber = CHINESE_NUM_MAP.get(seasonWord);
            }
        }

        if (seasonNumber == null) {
            if (!YEAR_PATTERN.matcher(showName).find()) {
                Matcher matchNum = SEASON_PATTERN_NUM.matcher(showName);
                if (matchNum.find()) {
                    String potentialName = matchNum.group(1).strip();
                    if (!potentialName.isEmpty()) {
                        showName = potentialName;
                        try {
                            seasonNumber = Integer.parseInt(matchNum.group(2));
                        } catch (NumberFormatException e) {
                            log.debug("解析数字季号失败", e);
                        }
                    }
                }
            }
        }*/

        return new SeriesInfo(showName, seasonNumber);
    }

    private List<MatchedItem> matchTitlesToTmdb(
            Set<String> titles, String itemType) {

        List<MatchedItem> matchedItems = new ArrayList<>();

        for (String title : titles) {
            if (StrUtil.isBlank(title)) {
                continue;
            }

            if ("Movie".equals(itemType)) {
                List<RTmdbMovie> results = tmdbProxy.getMovieByName(title, null);
                if (CollUtil.isEmpty(results)) {
                    continue;
                }
                RTmdbMovie bestMatch = CollUtil.getFirst(results);
                matchedItems.add(new MatchedItem(bestMatch.getId(), bestMatch.getTitle(), "Movie"));
            } else if ("Series".equals(itemType)) {
                SeriesInfo tvInfo = parseSeriesTitle(title);
                String showName = tvInfo.showName;
                // TODO 暂时不知道用来干嘛
                Integer seasonNumber = tvInfo.seasonNumber;
                try {
                    List<RTmdbTv> results = tmdbProxy.getTvByName(showName, null);
                    if (CollUtil.isEmpty(results)) {
                        continue;
                    }

                    RTmdbTv seriesResult = null;
                    String normShowName = normalizeString(showName);
                    // 精确匹配
                    for (RTmdbTv tv : results) {
                        String resultName = tv.getName();
                        if (StrUtil.equalsIgnoreCase(normalizeString(resultName), normShowName)) {
                            seriesResult = tv;
                            break;
                        }
                    }
                    // 未找到就用第一个
                    if (null == seriesResult) {
                        seriesResult = CollUtil.getFirst(results);
                    }
                    matchedItems.add(new MatchedItem(seriesResult.getId(), seriesResult.getName(), "Series"));

                } catch (Exception e) {
                    log.error("搜索TMDb剧集信息失败", e);
                }
            }
        }

        return matchedItems;
    }

    private static String normalizeString(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return s.replaceAll("[\\s:：·\\-*'!,?.。]+", "").toLowerCase();
    }
}