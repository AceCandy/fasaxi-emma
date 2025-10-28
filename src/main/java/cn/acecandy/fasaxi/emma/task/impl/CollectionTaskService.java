package cn.acecandy.fasaxi.emma.task.impl;

import cn.acecandy.fasaxi.emma.config.EmbyConfig;
import cn.acecandy.fasaxi.emma.dao.toolkit.entity.CustomCollections;
import cn.acecandy.fasaxi.emma.dao.toolkit.service.CustomCollectionsDao;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.proxy.EmbyProxy;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.json.JSONObject;
import cn.hutool.v7.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

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
        // if (StrUtil.equals(collectionType, "list")) {
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
}