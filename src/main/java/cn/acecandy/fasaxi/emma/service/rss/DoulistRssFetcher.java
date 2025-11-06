package cn.acecandy.fasaxi.emma.service.rss;

import cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType;
import cn.acecandy.fasaxi.emma.dao.embyboss.entity.TmdbProvider;
import cn.acecandy.fasaxi.emma.dao.embyboss.service.TmdbProviderDao;
import cn.acecandy.fasaxi.emma.sao.entity.MatchedItem;
import cn.acecandy.fasaxi.emma.sao.out.RTmdbMovie;
import cn.acecandy.fasaxi.emma.sao.out.RTmdbTv;
import cn.acecandy.fasaxi.emma.sao.proxy.DoubanProxy;
import cn.acecandy.fasaxi.emma.sao.proxy.TmdbProxy;
import cn.acecandy.fasaxi.emma.utils.HtmlUtil;
import cn.acecandy.fasaxi.emma.utils.ReUtil;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.core.collection.set.SetUtil;
import cn.hutool.v7.core.lang.mutable.MutablePair;
import cn.hutool.v7.core.text.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType.电影;
import static cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType.电视剧;

/**
 * 豆列 rss处理器
 *
 * @author tangningzhu
 * @since 2025/10/29
 */
@Slf4j
@Service
public class DoulistRssFetcher {


    @Resource
    private DoubanProxy doubanProxy;

    @Resource
    private TmdbProxy tmdbProxy;

    @Resource
    private TmdbProviderDao tmdbProviderDao;


    /**
     * 核心处理
     *
     * @param url 路径
     * @return {@link List }<{@link MatchedItem }>
     */
    public Set<MatchedItem> exec(String url, EmbyMediaType itemType) {
        List<MatchedItem.Doulist> allItems = ListUtil.of();
        if (StrUtil.contains(url, "douban.com/doulist")) {
            allItems = doubanProxy.getAllDoulist(url);
        } else if (StrUtil.contains(url, "douban/list")) {
            allItems = doubanProxy.getDoubanRss(url);
        }
        return matchTitlesAndIdToTmdb(allItems, itemType);
    }

    /**
     * 将标题和id匹配到tmdb
     *
     * @param allItems 所有物品
     * @param itemType 项目类型
     * @return {@link List }<{@link MatchedItem }>
     */
    private Set<MatchedItem> matchTitlesAndIdToTmdb(List<MatchedItem.Doulist> allItems,
                                                    EmbyMediaType itemType) {
        Set<MatchedItem> matchedItems = SetUtil.ofLinked();
        if (CollUtil.isEmpty(allItems)) {
            return matchedItems;
        }

        // 查找db中所有有豆瓣id的tmdb记录
        List<String> doubanIds = allItems.stream().map(MatchedItem.Doulist::doubanId).toList();
        List<TmdbProvider> tmdbProviders = tmdbProviderDao.findByDouban(doubanIds, itemType.name());
        Map<String, TmdbProvider> tmdbProviderMap = tmdbProviders.stream()
                .filter(t -> StrUtil.isNotBlank(t.getDoubanId())).collect(Collectors.toMap
                        (TmdbProvider::getDoubanId, Function.identity(), (_, v2) -> v2));

        allItems.forEach(i -> {
            String doubanId = i.doubanId();
            String title = i.title();
            Integer year = i.year();
            if (tmdbProviderMap.containsKey(doubanId)) {
                matchedItems.add(new MatchedItem(Integer.parseInt(tmdbProviderMap.get(doubanId).getTmdbId()),
                        title, itemType, null));
                return;
            }
            if (电影.equals(itemType)) {
                List<RTmdbMovie> results = tmdbProxy.getMovieByName(title, year);
                if (CollUtil.isNotEmpty(results)) {
                    RTmdbMovie bestMatch = CollUtil.getFirst(results);
                    matchedItems.add(new MatchedItem(bestMatch.getId(), bestMatch.getTitle(), itemType, bestMatch));
                }
            } else if (电视剧.equals(itemType)) {
                MutablePair<String, Integer> tvInfo = ReUtil.parseTvNameSeason(title);
                title = tvInfo.getLeft();
                // TODO 暂时不知道用来干嘛
                Integer seasonNumber = tvInfo.getRight();

                List<RTmdbTv> results = tmdbProxy.getTvByName(title, seasonNumber, year);
                if (CollUtil.isNotEmpty(results)) {
                    RTmdbTv bestMatch = null;
                    String normShowName = HtmlUtil.normalizeString(title);
                    // 精确匹配
                    for (RTmdbTv tv : results) {
                        String resultName = tv.getName();
                        if (StrUtil.equalsIgnoreCase(HtmlUtil.normalizeString(resultName), normShowName)) {
                            bestMatch = tv;
                            break;
                        }
                    }
                    // 未找到就用第一个
                    if (null == bestMatch) {
                        bestMatch = CollUtil.getFirst(results);
                    }
                    matchedItems.add(new MatchedItem(bestMatch.getId(), bestMatch.getName(), itemType, bestMatch));
                }
            }
        });
        return matchedItems;
    }

}