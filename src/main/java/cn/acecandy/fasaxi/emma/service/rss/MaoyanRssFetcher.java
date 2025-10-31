package cn.acecandy.fasaxi.emma.service.rss;

import cn.acecandy.fasaxi.emma.sao.entity.MatchedItem;
import cn.acecandy.fasaxi.emma.sao.out.RTmdbMovie;
import cn.acecandy.fasaxi.emma.sao.out.RTmdbTv;
import cn.acecandy.fasaxi.emma.sao.proxy.MaoyanProxy;
import cn.acecandy.fasaxi.emma.sao.proxy.TmdbProxy;
import cn.acecandy.fasaxi.emma.utils.HtmlUtil;
import cn.acecandy.fasaxi.emma.utils.ReUtil;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.core.collection.set.SetUtil;
import cn.hutool.v7.core.lang.mutable.MutablePair;
import cn.hutool.v7.core.map.MapUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.core.text.split.SplitUtil;
import cn.hutool.v7.json.JSONArray;
import cn.hutool.v7.json.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType.电影;
import static cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType.电视剧;
import static cn.acecandy.fasaxi.emma.sao.proxy.MaoyanProxy.PLATFORM_MAP;
import static cn.acecandy.fasaxi.emma.sao.proxy.MaoyanProxy.TV_HEAT_MAP;
import static cn.hutool.v7.core.text.StrPool.COMMA;

/**
 * 猫眼rss处理器
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

    /**
     * 有效猫眼平台
     */
    private static final List<String> VALID_MAOYAN_PLATFORMS =
            List.of("tencent", "iqiyi", "youku", "mango");

    /**
     * 核心处理
     *
     * @param url 网址
     * @return {@link List }<{@link MatchedItem }>
     */
    public List<MatchedItem> exec(String url) {
        if (StrUtil.isBlank(url)) {
            return ListUtil.of();
        }
        List<String> keys = SplitUtil.splitTrim(StrUtil.removePrefix(url, "maoyan://"), COMMA);
        if (CollUtil.isNotEmpty(keys)) {
            return ListUtil.of();
        }
        String platform = CollUtil.getLast(SplitUtil.splitTrim(CollUtil.getFirst(keys), "-"));
        if (!VALID_MAOYAN_PLATFORMS.contains(platform)) {
            platform = "all";
        }

        MutablePair<Set<String>, Set<String>> topTitles =
                getMaoyanTopTitles(keys, platform);

        List<MatchedItem> matchedMovies = matchTitlesToTmdb(
                topTitles.getLeft(), "Movie");

        List<MatchedItem> matchedSeries = matchTitlesToTmdb(
                topTitles.getRight(), "Series");

        List<MatchedItem> allItems = ListUtil.of();
        allItems.addAll(matchedMovies);
        allItems.addAll(matchedSeries);
        return allItems;
    }

    /**
     * 获取猫眼榜单的所有电影+电视剧标题
     *
     * @param keys     钥匙
     * @param platform 平台
     * @return {@link MutablePair }<{@link Set }<{@link String }>, {@link Set }<{@link String }>>
     */
    private MutablePair<Set<String>, Set<String>> getMaoyanTopTitles(List<String> keys,
                                                                     String platform) {
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

    /**
     * 将标题与tmdb匹配
     *
     * @param titles   标题
     * @param itemType 项目类型
     * @return {@link List }<{@link MatchedItem }>
     */
    private List<MatchedItem> matchTitlesToTmdb(Set<String> titles, String itemType) {
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
                matchedItems.add(new MatchedItem(bestMatch.getId(), bestMatch.getTitle(), 电影));
            } else if ("Series".equals(itemType)) {
                MutablePair<String, Integer> tvInfo = ReUtil.parseMaoyanTvNameSeason(title);
                String showName = tvInfo.getLeft();
                // TODO 暂时不知道用来干嘛
                Integer seasonNumber = tvInfo.getRight();
                try {
                    List<RTmdbTv> results = tmdbProxy.getTvByName(showName, null);
                    if (CollUtil.isEmpty(results)) {
                        continue;
                    }

                    RTmdbTv seriesResult = null;
                    String normShowName = HtmlUtil.normalizeString(showName);
                    // 精确匹配
                    for (RTmdbTv tv : results) {
                        String resultName = tv.getName();
                        if (StrUtil.equalsIgnoreCase(HtmlUtil.normalizeString(resultName), normShowName)) {
                            seriesResult = tv;
                            break;
                        }
                    }
                    // 未找到就用第一个
                    if (null == seriesResult) {
                        seriesResult = CollUtil.getFirst(results);
                    }
                    matchedItems.add(new MatchedItem(seriesResult.getId(), seriesResult.getName(), 电视剧));

                } catch (Exception e) {
                    log.error("搜索TMDb剧集信息失败", e);
                }
            }
        }

        return matchedItems;
    }

}