package cn.acecandy.fasaxi.emma.service.rss;

import cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType;
import cn.acecandy.fasaxi.emma.dao.embyboss.entity.TmdbProvider;
import cn.acecandy.fasaxi.emma.dao.embyboss.service.TmdbProviderDao;
import cn.acecandy.fasaxi.emma.sao.entity.MatchedItem;
import cn.acecandy.fasaxi.emma.sao.out.DataEyeContent;
import cn.acecandy.fasaxi.emma.sao.out.EmbyItem;
import cn.acecandy.fasaxi.emma.sao.out.RTmdbMovie;
import cn.acecandy.fasaxi.emma.sao.out.RTmdbTv;
import cn.acecandy.fasaxi.emma.sao.proxy.DataEyeProxy;
import cn.acecandy.fasaxi.emma.sao.proxy.DoubanProxy;
import cn.acecandy.fasaxi.emma.sao.proxy.EmbyProxy;
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
 * dataeye 短剧 rss处理器
 *
 * @author tangningzhu
 * @since 2025/10/29
 */
@Slf4j
@Service
public class DataEyeRssFetcher {


    @Resource
    private DataEyeProxy dataEyeProxy;

    @Resource
    private EmbyProxy embyProxy;

    /**
     * 库ID
     */
    private static final Integer PARENT_ID = 545990;

    /**
     * 核心处理
     *
     * @param url 路径
     * @return {@link List }<{@link MatchedItem }>
     */
    public Set<String> exec(String url) {
        List<DataEyeContent> top30 = dataEyeProxy.getTop30();
        if (CollUtil.isEmpty(top30)) {
            return SetUtil.ofLinked();
        }

        Set<String> matchedItems = SetUtil.ofLinked();
        top30.forEach(t -> {
            String name = t.getPlayletName();

            List<EmbyItem> items = embyProxy.searchItemsByCollections(name, "Series", PARENT_ID);
            if (CollUtil.isEmpty(items)) {
                name = t.getOverseasPlayletName();
                items = embyProxy.searchItemsByCollections(name, "Series", PARENT_ID);
                if (CollUtil.isEmpty(items)) {
                    return;
                }
            }

            EmbyItem bestMatch = CollUtil.getFirst(items);
            matchedItems.add(bestMatch.getItemId());
        });
        return matchedItems;
    }

}