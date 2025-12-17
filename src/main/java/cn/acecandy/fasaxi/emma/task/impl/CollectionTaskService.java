package cn.acecandy.fasaxi.emma.task.impl;

import cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType;
import cn.acecandy.fasaxi.emma.config.EmbyConfig;
import cn.acecandy.fasaxi.emma.dao.toolkit.dto.GeneratedMediaInfo;
import cn.acecandy.fasaxi.emma.dao.toolkit.entity.CustomCollections;
import cn.acecandy.fasaxi.emma.dao.toolkit.entity.MediaMetadata;
import cn.acecandy.fasaxi.emma.dao.toolkit.service.CustomCollectionsDao;
import cn.acecandy.fasaxi.emma.dao.toolkit.service.MediaMetadataDao;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.entity.MatchedItem;
import cn.acecandy.fasaxi.emma.sao.out.EmbyItem;
import cn.acecandy.fasaxi.emma.sao.proxy.EmbyProxy;
import cn.acecandy.fasaxi.emma.service.rss.DataEyeRssFetcher;
import cn.acecandy.fasaxi.emma.service.rss.DoulistRssFetcher;
import cn.acecandy.fasaxi.emma.service.rss.MaoyanRssFetcher;
import cn.acecandy.fasaxi.emma.service.rss.RuleFilterFetcher;
import cn.acecandy.fasaxi.emma.utils.CacheUtil;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.core.collection.set.SetUtil;
import cn.hutool.v7.core.date.DateUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.core.text.split.SplitUtil;
import cn.hutool.v7.json.JSONObject;
import cn.hutool.v7.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
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
    private MediaMetadataDao mediaMetadataDao;

    @Resource
    private EmbyProxy embyProxy;

    @Resource
    private RedisClient redisClient;

    @Resource
    private MaoyanRssFetcher maoyanRssFetcher;

    @Resource
    private DoulistRssFetcher doulistRssFetcher;

    @Resource
    private RuleFilterFetcher ruleFilterFetcher;

    @Resource
    private DataEyeRssFetcher dataEyeRssFetcher;

    public void syncQuickCollection() {
        List<CustomCollections> collections = customCollectionsDao.findAllByStatus("active");
        if (CollUtil.isEmpty(collections)) {
            return;
        }
        collections.forEach((v) -> {
            long start = System.currentTimeMillis();
            try {
                obtainCollection(v.getId());
            } catch (Exception e) {
                log.warn("[同步自定义合集-{}:{}] 异常！", v.getName(), v.getId(), e);
            } finally {
                log.warn("[同步自定义合集-{}:{}] 执行耗时: {}ms", v.getName(), v.getId(),
                        System.currentTimeMillis() - start);
            }
        });
    }

    public void syncSlowCollection() {
        List<CustomCollections> collections = customCollectionsDao.findAllByStatus("paused");
        if (CollUtil.isEmpty(collections)) {
            return;
        }
        collections.forEach((v) -> {
            long start = System.currentTimeMillis();
            try {
                obtainCollection(v.getId());
            } catch (Exception e) {
                log.warn("[同步自定义合集-{}:{}] 异常！", v.getName(), v.getId(), e);
            } finally {
                log.warn("[同步自定义合集-{}:{}] 执行耗时: {}ms", v.getName(), v.getId(),
                        System.currentTimeMillis() - start);
            }
        });
    }

    public void obtainCollection(Long id) {
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
        List<String> itemTypeFromDb = JSONUtil.toList(coll.getItemType(), String.class);
        EmbyMediaType itemType = EmbyMediaType.fromEmby(CollUtil.getFirst(itemTypeFromDb));

        // Integer limit = definition.containsKey("limit") ? definition.getInt("limit") : 50;
        List<String> newEmbyIds = ListUtil.of();
        List<GeneratedMediaInfo> generatedMediaInfos = ListUtil.of();
        if (StrUtil.equals(collectionType, "emby-list")) {
            newEmbyIds.addAll(handleEmbyCollectionList(definition));
            if (CollUtil.isEmpty(newEmbyIds)) {
                return;
            }

            generatedMediaInfos.addAll(newEmbyIds.stream()
                    .map(m -> GeneratedMediaInfo.builder()
                            .status("in_library").embyId(m).build()
                    ).toList());
        } else {
            Set<MatchedItem> matchedItems = SetUtil.ofLinked();
            if (StrUtil.equals(collectionType, "list")) {
                matchedItems = handleCollectionList(definition);
            } else if (StrUtil.equals(collectionType, "filter")) {
                matchedItems = handleCollectionFilter(definition, itemType);
            }
            // TODO 修正匹配逻辑
            if (CollUtil.isEmpty(matchedItems)) {
                return;
            }
            List<String> tmdbIds = matchedItems.stream().map(s -> String.valueOf(s.id())).toList();
            // 默认合集存在，如果不存在从页面进行创建
            List<MediaMetadata> mediaMetadatas = mediaMetadataDao.findByTmdbId(tmdbIds, itemType.getEmbyName());
            // 内存重排序为传入的顺序
            Map<String, MediaMetadata> metadataMap = mediaMetadatas.stream().collect(Collectors.toMap(
                    MediaMetadata::getTmdbId, Function.identity(), (_, v2) -> v2));
            mediaMetadatas = tmdbIds.stream()
                    .map(metadataMap::get).filter(Objects::nonNull).toList();

            newEmbyIds.addAll(mediaMetadatas.stream()
                    .map(MediaMetadata::getEmbyItemIdsJson).filter(CollUtil::isNotEmpty)
                    .flatMap(List::stream).filter(StrUtil::isNotBlank)
                    .toList());

            generatedMediaInfos.addAll(mediaMetadatas.stream()
                    .flatMap(m -> m.getEmbyItemIdsJson().stream().filter(StrUtil::isNotBlank)
                            .map(embyId -> GeneratedMediaInfo.builder()
                                    .title(m.getTitle()).status(m.getInLibrary() ? "in_library" : "missing")
                                    .embyId(embyId).tmdbId(m.getTmdbId())
                                    .releaseDate(DateUtil.formatDate(m.getReleaseDate()))
                                    .build())
                    ).toList());
        }

        String embyCollectionId = coll.getEmbyCollectionId();
        if (StrUtil.isBlank(embyCollectionId)) {
            embyCollectionId = embyProxy.createCollections(coll.getName(), newEmbyIds);
            if (StrUtil.isBlank(embyCollectionId)) {
                log.warn("[创建新合集-name:{}] 失败", coll.getName());
                return;
            }
        } else {
            List<EmbyItem> items = embyProxy.getItemsByCollections(embyCollectionId);
            List<String> existEmbyIds = items.stream().map(EmbyItem::getItemId).toList();

            List<String> needAddEmbyIds = CollUtil.subtract(newEmbyIds, existEmbyIds).stream().toList();
            List<String> needDeleteEmbyIds = CollUtil.subtract(existEmbyIds, newEmbyIds).stream().toList();
            embyProxy.delItemsByCollections(embyCollectionId, needDeleteEmbyIds);
            embyProxy.addItemsByCollections(embyCollectionId, needAddEmbyIds);
        }
        CustomCollections.x().setId(id).setHealthStatus("ok").setMissingCount(0)
                .setGeneratedMediaInfoJson(generatedMediaInfos)
                .setInLibraryCount(CollUtil.size(newEmbyIds))
                .setEmbyCollectionId(embyCollectionId)
                .setLastSyncedAt(DateUtil.now()).updateById();
        // }
        // return;
    }

    /**
     * 处理集合list（无tmdbid）
     *
     * @param definition 定义
     * @return {@link List }<{@link MatchedItem }>
     */
    private Set<String> handleEmbyCollectionList(JSONObject definition) {
        String url = definition.getStr("url");
        if (StrUtil.isBlank(url)) {
            return Set.of();
        }
        List<String> itemTypeFromDb = JSONUtil.toList(definition.getStr("item_type"), String.class);
        EmbyMediaType itemType = EmbyMediaType.fromEmby(CollUtil.getFirst(itemTypeFromDb));
        if (StrUtil.contains(url, "playlet-applet.dataeye.com")) {
            return dataEyeRssFetcher.exec(url);
        }
        return Set.of();
    }

    /**
     * 处理集合list
     *
     * @param definition 定义
     * @return {@link List }<{@link MatchedItem }>
     */
    private Set<MatchedItem> handleCollectionList(JSONObject definition) {
        String url = definition.getStr("url");
        if (StrUtil.isBlank(url)) {
            return Set.of();
        }
        /*if (StrUtil.startWith(url, "http://192.168.1.249:11200")) {
            url = StrUtil.replace(url, "http://192.168.1.249:11200",
                    "https://rss.acecandy.cn:880");
        }*/

        List<String> itemTypeFromDb = JSONUtil.toList(definition.getStr("item_type"), String.class);
        EmbyMediaType itemType = EmbyMediaType.fromEmby(CollUtil.getFirst(itemTypeFromDb));
        if (StrUtil.startWith(url, "maoyan://")) {
            return maoyanRssFetcher.exec(url);
        } else if (StrUtil.contains(url, "douban")) {
            return doulistRssFetcher.exec(url, itemType);
        }
        return Set.of();
    }

    /**
     * 处理合集filter
     *
     * @param definition 定义
     * @return {@link List }<{@link MatchedItem }>
     */
    private Set<MatchedItem> handleCollectionFilter(JSONObject definition, EmbyMediaType itemType) {
        List<String> targetLibraryIds = JSONUtil.toList(definition.getStr("target_library_ids"), String.class);
        if (CollUtil.isEmpty(targetLibraryIds)) {
            return SetUtil.of();
        }
        List<String> allItemId = getAllItemIdByCache(targetLibraryIds);
        // 转成本地db中的item 一起查可能id太多 分成1000个一组查询
        List<MediaMetadata> allItems = CollUtil.partition(allItemId, 4000).stream()
                .map(i -> mediaMetadataDao.findByEmbyId(i, itemType.getEmbyName()))
                .flatMap(Collection::stream).toList();

        Set<MatchedItem> matchedItems = ruleFilterFetcher.exec(allItems, definition, itemType);
        return matchedItems;
    }

    private List<EmbyItem> getAllItemsByCache(List<String> libraryIds) {
        List<EmbyItem> allItems = ListUtil.ofCopyOnWrite();

        List<String> allLibraryIds = embyConfig.getVirtualHide().keySet().stream().toList();
        List<String> realLibraryIds = CollUtil.isEmpty(libraryIds) ? allLibraryIds :
                CollUtil.intersectionDistinct(libraryIds, allLibraryIds).stream().toList();
        realLibraryIds.parallelStream().forEach(k -> {
            List<EmbyItem> items = redisClient.getBean(CacheUtil.buildItemsCacheKey(k));
            allItems.addAll(items);
        });
        return allItems;
    }

    public List<String> getAllItemIdByCache(List<String> libraryIds) {
        List<String> allItemsId = ListUtil.ofCopyOnWrite();

        List<String> allLibraryIds = embyConfig.getVirtualHide().keySet().stream().toList();
        List<String> realLibraryIds = CollUtil.isEmpty(libraryIds) ? allLibraryIds :
                CollUtil.intersectionDistinct(libraryIds, allLibraryIds).stream().toList();
        realLibraryIds.parallelStream().forEach(k -> {
            String itemIds = redisClient.getStr(CacheUtil.buildItemsIdCacheKey(k));
            allItemsId.addAll(SplitUtil.splitTrim(itemIds, ","));
        });
        return allItemsId;
    }
}