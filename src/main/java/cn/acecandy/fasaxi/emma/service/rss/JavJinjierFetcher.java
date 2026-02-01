package cn.acecandy.fasaxi.emma.service.rss;

import cn.acecandy.fasaxi.emma.dao.embyboss.entity.JavJinjier;
import cn.acecandy.fasaxi.emma.dao.embyboss.service.JavJinjierDao;
import cn.acecandy.fasaxi.emma.sao.entity.MatchedItem;
import cn.acecandy.fasaxi.emma.sao.out.EmbyItem;
import cn.acecandy.fasaxi.emma.sao.proxy.EmbyProxy;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.collection.set.SetUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.core.text.split.SplitUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * jav jinjier 处理器
 *
 * @author tangningzhu
 * @since 2025/10/29
 */
@Slf4j
@Service
public class JavJinjierFetcher {


    @Resource
    private EmbyProxy embyProxy;

    @Resource
    private JavJinjierDao javJinjierDao;

    /**
     * 库ID
     */
    private static final Integer PARENT_ID = 294462;

    /**
     * 核心处理
     * <p>
     * JavJinjier=all
     *
     * @param url 路径
     * @return {@link List }<{@link MatchedItem }>
     */
    public Set<String> exec(String url) {
        List<JavJinjier> ranks = javJinjierDao.list();
        if (CollUtil.isEmpty(ranks)) {
            return SetUtil.ofLinked();
        }

        Set<String> matchedItems = SetUtil.ofLinked();
        ranks.forEach(t -> {
            String name = t.getSerNum();

            List<EmbyItem> items = embyProxy.searchItemsByCollections(name, "Movie", PARENT_ID);
            if (CollUtil.isEmpty(items)) {
                return;
            }

            EmbyItem bestMatch = CollUtil.getFirst(items);
            String bestMatchName = CollUtil.getFirst(SplitUtil.splitTrim(bestMatch.getName(), " "));
            if (!StrUtil.equals(bestMatchName, name)) {
                return;
            }
            matchedItems.add(bestMatch.getItemId());
        });
        return matchedItems;
    }

}