package cn.acecandy.fasaxi.emma.service.rss;

import cn.acecandy.fasaxi.emma.sao.entity.MatchedItem;
import cn.acecandy.fasaxi.emma.sao.out.DataEyeRank;
import cn.acecandy.fasaxi.emma.sao.out.EmbyItem;
import cn.acecandy.fasaxi.emma.sao.proxy.DataEyeProxy;
import cn.acecandy.fasaxi.emma.sao.proxy.EmbyProxy;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.collection.set.SetUtil;
import cn.hutool.v7.core.date.DateFormatPool;
import cn.hutool.v7.core.date.DateUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

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
        String lastMonth = DateFormatPool.NORM_MONTH_FORMAT.format(DateUtil.lastMonth());
        List<DataEyeRank> top30 = dataEyeProxy.getHotRank(lastMonth);
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