package cn.acecandy.fasaxi.emma.task.impl;

import cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType;
import cn.acecandy.fasaxi.emma.config.EmbyConfig;
import cn.acecandy.fasaxi.emma.dao.toolkit.entity.CustomCollections;
import cn.acecandy.fasaxi.emma.dao.toolkit.service.CustomCollectionsDao;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.entity.MatchedItem;
import cn.acecandy.fasaxi.emma.sao.proxy.EmbyProxy;
import cn.acecandy.fasaxi.emma.service.rss.DoulistRssFetcher;
import cn.acecandy.fasaxi.emma.service.rss.MaoyanRssFetcher;
import cn.acecandy.fasaxi.emma.utils.CacheUtil;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.core.text.split.SplitUtil;
import cn.hutool.v7.json.JSONObject;
import cn.hutool.v7.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 自定义合集任务 实现
 *
 * @author tangningzhu
 * @since 2025/3/3
 */
@Slf4j
@Component
public class CollectionTaskService {

    @Resource
    private EmbyConfig embyConfig;

    @Resource
    private CustomCollectionsDao customCollectionsDao;

    @Resource
    private EmbyProxy embyProxy;

    @Resource
    private RedisClient redisClient;

    @Resource
    private MaoyanRssFetcher maoyanRssFetcher;

    @Resource
    private DoulistRssFetcher doulistRssFetcher;

    public void syncCollection() {
        List<CustomCollections> collections = customCollectionsDao.findAllByStatus("active");
        if (CollUtil.isEmpty(collections)) {
            return;
        }
        collections.forEach((v) -> {
            long start = System.currentTimeMillis();
            try {
                obtainCollectionToRedis(v.getId());
            } catch (Exception e) {
                log.warn("[同步自定义合集-{}:{}] 异常！", v.getName(), v.getId(), e);
            } finally {
                log.warn("[同步自定义合集-{}:{}] 执行耗时: {}ms", v.getName(), v.getId(),
                        System.currentTimeMillis() - start);
            }
        });
    }

    private void obtainCollectionToRedis(Long id) {
        if (null == id) {
            return;
        }
        CustomCollections coll = customCollectionsDao.getById(id);
        if (null == coll) {
            log.warn("[同步自定义合集-id:{}] 不存在！", id);
            return;
        }
        String collectionType = coll.getType();
        JSONObject definition = JSONUtil.parseObj(coll.getDefinitionJson());
        // Integer limit = definition.containsKey("limit") ? definition.getInt("limit") : 50;
        if (StrUtil.equals(collectionType, "list")) {
            handleCollectionList(definition);
        } else {

        }
        //     // 列表类型合集
        //     ListImporter importer = new ListImporter(processor.getTmdbApiKey());
        //     ImportResult result = importer.process(definition);
        //     tmdbItems = result.getTmdbItems();
        // } else if (StrUtil.equals(collectionType, "filter")) {
        //     // 筛选器类型合集
        //     FilterEngine engine = new FilterEngine();
        //     tmdbItems = engine.executeFilter(definition);
        // }


        // List<EmbyItem> items = embyProxy.getItemsByParentIdOnLock(parentId);
        // if (CollUtil.isEmpty(items)) {
        //     return;
        // }
        // redisClient.set(CacheUtil.buildItemsCacheKey(parentId), items);
        // redisClient.set(CacheUtil.buildItemsIdCacheKey(parentId),
        //         StrUtil.join(COMMA, items.stream().map(EmbyItem::getItemId).toList()));
    }

    /**
     * 处理集合list
     *
     * @param definition 定义
     * @return {@link List }<{@link MatchedItem }>
     */
    private List<MatchedItem> handleCollectionList(JSONObject definition) {
        String url = definition.getStr("url");
        if (StrUtil.isBlank(url)) {
            return ListUtil.of();
        }
        List<String> itemTypeFromDb = JSONUtil.toList(definition.getStr("item_type"), String.class);
        EmbyMediaType itemType = EmbyMediaType.fromEmby(CollUtil.getFirst(itemTypeFromDb));
        if (StrUtil.startWith(url, "maoyan://")) {
            return maoyanRssFetcher.exec(url);
        } else if (StrUtil.contains(url, "douban.com/doulist")) {
            return doulistRssFetcher.exec(url, itemType);
        }
        return ListUtil.of();
    }

    /**
     * 处理合集filter
     *
     * @param definition 定义
     * @return {@link List }<{@link MatchedItem }>
     */
    private List<MatchedItem> handleCollectionFilter(JSONObject definition) {
        List<String> targetLibraryIds = JSONUtil.toList(definition.getStr("target_library_ids"), String.class);
        if (CollUtil.isEmpty(targetLibraryIds)) {
            return ListUtil.of();
        }
        List<String> allItems = getAllItemsByCache(targetLibraryIds);

        // --- 后续的筛选逻辑保持不变 ---
        logger.info(String.format("  ➜ 已加载 %d 条元数据，开始应用筛选规则...", allMediaMetadata.size()));

        for (Map<String, Object> mediaMetadata : allMediaMetadata) {
            if (itemMatchesRules(mediaMetadata, rules, logic)) {
                String tmdbId = (String) mediaMetadata.get("tmdb_id");
                String itemType = (String) mediaMetadata.get("item_type");

                if (tmdbId != null && itemType != null) {
                    Map<String, String> matchedItem = new HashMap<>();
                    matchedItem.put("id", tmdbId);
                    matchedItem.put("type", itemType);
                    matchedItems.add(matchedItem);
                }
            }
        }

        // 去重
        List<Map<String, String>> uniqueItems = matchedItems.stream()
                .collect(Collectors.toMap(
                        item -> item.get("type") + "-" + item.get("id"),
                        item -> item,
                        (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .collect(Collectors.toList());

        logger.info(String.format("  ➜ 筛选完成！共找到 %d 部匹配的媒体项目。", uniqueItems.size()));
        return uniqueItems;
    }

    private List<String> getAllItemsByCache(List<String> libraryIds) {
        List<String> allItemsId = ListUtil.ofCopyOnWrite();

        List<String> allLibraryIds = embyConfig.getVirtualHide().keySet().stream().toList();
        List<String> realLibraryIds = CollUtil.isEmpty(libraryIds) ? allLibraryIds :
                CollUtil.intersectionDistinct(libraryIds, allLibraryIds).stream().toList();
        realLibraryIds.parallelStream().forEach(k -> {
            String result = redisClient.getStr(CacheUtil.buildItemsCacheKey(k));
            allItemsId.addAll(SplitUtil.splitTrim(result, ","));
        });
        return allItemsId;
    }
}