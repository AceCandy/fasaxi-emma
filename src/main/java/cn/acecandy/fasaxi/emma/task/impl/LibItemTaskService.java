package cn.acecandy.fasaxi.emma.task.impl;

import cn.acecandy.fasaxi.emma.config.EmbyConfig;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.out.EmbyItem;
import cn.acecandy.fasaxi.emma.sao.proxy.EmbyProxy;
import cn.acecandy.fasaxi.emma.utils.CacheUtil;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.text.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static cn.hutool.v7.core.text.StrPool.COMMA;

/**
 * 库媒体项定时任务 实现
 *
 * @author tangningzhu
 * @since 2025/3/3
 */
@Slf4j
@Component
public class LibItemTaskService {

    @Resource
    private EmbyConfig embyConfig;

    @Resource
    private EmbyProxy embyProxy;

    @Resource
    private RedisClient redisClient;

    public void syncLibItems() {
        Map<String, String> virtualHide = embyConfig.getVirtualHide();
        virtualHide.forEach((k, v) -> {
            long start = System.currentTimeMillis();
            try {
                obtainItemsToRedis(k);
            } catch (Exception e) {
                log.warn("[同步库的媒体项-{}:{}] 异常！", v, k, e);
            } finally {
                log.warn("[同步库的媒体项-{}:{}] 执行耗时: {}ms", v, k, System.currentTimeMillis() - start);
            }
        });
    }

    private void obtainItemsToRedis(String parentId) {
        if (StrUtil.isBlank(parentId)) {
            return;
        }
        List<EmbyItem> items = embyProxy.getItemsByParentIdOnLock(parentId);
        if (CollUtil.isEmpty(items)) {
            return;
        }
        redisClient.set(CacheUtil.buildItemsCacheKey(parentId), items);
        redisClient.set(CacheUtil.buildItemsIdCacheKey(parentId),
                StrUtil.join(COMMA, items.stream().map(EmbyItem::getItemId).toList()));
    }
}