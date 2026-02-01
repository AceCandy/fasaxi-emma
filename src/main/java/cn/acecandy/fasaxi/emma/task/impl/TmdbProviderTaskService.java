package cn.acecandy.fasaxi.emma.task.impl;

import cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType;
import cn.acecandy.fasaxi.emma.dao.embyboss.entity.TmdbProvider;
import cn.acecandy.fasaxi.emma.dao.embyboss.service.JavRankDao;
import cn.acecandy.fasaxi.emma.dao.embyboss.service.TmdbProviderDao;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.out.EmbyItem;
import cn.acecandy.fasaxi.emma.sao.proxy.DoubanProxy;
import cn.acecandy.fasaxi.emma.sao.proxy.EmbyProxy;
import cn.acecandy.fasaxi.emma.utils.ThreadUtil;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.core.date.DateTime;
import cn.hutool.v7.core.math.NumberUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.core.util.RandomUtil;
import cn.hutool.v7.http.client.engine.ClientEngine;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;


/**
 * tmdb provider任务相关 实现
 *
 * @author tangningzhu
 * @since 2025/3/3
 */
@Slf4j
@Component
public class TmdbProviderTaskService {

    @Resource
    private JavRankDao javRankDao;

    @Resource
    private RedisClient redisClient;

    @Resource
    private ClientEngine httpClient;

    @Resource
    private CollectionTaskService collectionTaskService;

    @Resource
    private EmbyProxy embyProxy;

    @Resource
    private TmdbProviderDao tmdbProviderDao;

    @Resource
    private DoubanProxy doubanProxy;

    /**
     * 同步项目信息（从当前的source_id的最大值+1开始）
     *
     */
    public void syncTmdbProvider(Integer min, Integer max) {
        String uniqueKey = "unique:tmdbId";
        List<Integer> allItemIds = ListUtil.of();
        if (null == min || null == max) {
            allItemIds = collectionTaskService.getAllItemIdByCacheNoShort(null)
                    .stream().map(NumberUtil::parseInt).toList();
        } else {
            allItemIds = IntStream.rangeClosed(min, max).boxed().toList();
        }
        processItemsWith3Threads(allItemIds, uniqueKey);

        log.warn("[同步完成] 补充构建缺失的tmdbProvider");
    }

    public void processItemsWith3Threads(List<Integer> allItemIds, String uniqueKey) {
        // 1. 创建固定3个线程的线程池（Hutool/原生都可，这里用原生更简洁）
        ExecutorService executor = Executors.newFixedThreadPool(5);

        // 2. 遍历所有itemId，异步提交到线程池（无需等待返回）
        for (Integer itemId : allItemIds) {
            // 注意：捕获itemId的当前值（lambda闭包陷阱）
            Integer finalItemId = itemId;
            executor.submit(() -> {
                String value = redisClient.hgetStr(uniqueKey, finalItemId.toString());
                if (StrUtil.isNotBlank(value)) {
                    System.out.print(".");
                    return;
                }
                try {
                    // 核心处理逻辑
                    List<EmbyItem> embyItems = embyProxy.getItemInfoByCache(finalItemId.toString());
                    if (CollUtil.isEmpty(embyItems)) {
                        return;
                    }

                    EmbyItem embyItem = CollUtil.getFirst(embyItems);
                    if (embyProxy.initTmdbProvider(embyItem)) {
                        redisClient.hset(uniqueKey, finalItemId.toString(), "1");
                    }
                } catch (Exception e) {
                    log.warn("Controller-[itemId:{}]构建tmdb-douban失败: ", finalItemId, e);
                } finally {
                    // 每个任务执行完休眠10-50ms（保留原有逻辑）
                    ThreadUtil.safeSleep(RandomUtil.randomInt(20, 50));
                }
            });
        }

        // 3. 优雅关闭线程池（非必须，但建议加，避免线程泄漏）
        ThreadUtil.execAsync(() -> {
            executor.shutdown(); // 停止接收新任务
            try {
                // 等待所有任务执行完（可根据业务调整超时时间）
                executor.awaitTermination(1, java.util.concurrent.TimeUnit.HOURS);
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        });
    }

    public void completionDoubanId() {
        String uniqueKey = "unique:imdbId-doubanId";
        List<TmdbProvider> tmdbProviders = tmdbProviderDao.findAllImdbNoDouBan();

        AtomicInteger i = new AtomicInteger();
        tmdbProviders.forEach(tmdbProvider -> {
            String imdbId = tmdbProvider.getImdbId();
            EmbyMediaType type = EmbyMediaType.fromEmby(tmdbProvider.getEmbyType());
            String value = redisClient.hgetStr(uniqueKey, imdbId);
            if (StrUtil.isNotBlank(value)) {
                return;
            }
            try {
                String doubanId = doubanProxy.getDoubanIdByImdbId(type, imdbId);
                if (StrUtil.isNotBlank(doubanId)) {
                    tmdbProvider.setDoubanId(doubanId);
                    tmdbProvider.setUpdateTime(new DateTime());
                    tmdbProviderDao.updateById(tmdbProvider);
                }
                redisClient.hset(uniqueKey, imdbId, "1");
                i.getAndIncrement();
            } catch (Exception e) {
                log.warn("Controller-[itemId:{}]构建tmdb-douban-补全豆瓣id失败: ", imdbId, e);
            } finally {
                ThreadUtil.safeSleep(RandomUtil.randomInt(2_500, 4_500));
            }
        });
        log.warn("构建tmdb&豆瓣本地库(补全豆瓣id)==>执行完成, 共处理: {}条", i.get());
    }
}