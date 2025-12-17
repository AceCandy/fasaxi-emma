package cn.acecandy.fasaxi.emma.control;

import cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType;
import cn.acecandy.fasaxi.emma.common.resp.Rsres;
import cn.acecandy.fasaxi.emma.dao.embyboss.entity.EmbyItemPic;
import cn.acecandy.fasaxi.emma.dao.embyboss.entity.TmdbProvider;
import cn.acecandy.fasaxi.emma.dao.embyboss.service.EmbyItemPicDao;
import cn.acecandy.fasaxi.emma.dao.embyboss.service.TmdbProviderDao;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.out.EmbyItem;
import cn.acecandy.fasaxi.emma.sao.out.EmbyItemsInfoOut;
import cn.acecandy.fasaxi.emma.sao.proxy.DoubanProxy;
import cn.acecandy.fasaxi.emma.sao.proxy.EmbyProxy;
import cn.acecandy.fasaxi.emma.task.impl.CollectionTaskService;
import cn.acecandy.fasaxi.emma.utils.ThreadUtil;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.core.date.DateTime;
import cn.hutool.v7.core.lang.Console;
import cn.hutool.v7.core.math.NumberUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.core.util.RandomUtil;
import cn.hutool.v7.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Slf4j
@RestController
@RequestMapping("/api")
public class ApiController {

    @Resource
    private EmbyItemPicDao embyItemPicDao;

    @Resource
    private TmdbProviderDao tmdbProviderDao;

    @Resource
    private EmbyProxy embyProxy;

    @Resource
    private DoubanProxy doubanProxy;

    @Resource
    private RedisClient redisClient;

    @Resource
    private CollectionTaskService collectionTaskService;

    // 当前系统时间
    @GetMapping("/time")
    public Rsres<Object> health() {
        return Rsres.success(System.currentTimeMillis());
    }

    // 更新对应合集
    @GetMapping("/obtainCollection")
    public Rsres<Void> obtainCollection(Long collectionId) {
        collectionTaskService.obtainCollection(collectionId);
        return Rsres.success();
    }

    // 清除db无用图片
    @GetMapping("/clear/db-pic")
    public Rsres<Object> clearDbPic() {
        int removeCnt = 0;
        int pageNum = 1;
        int pageSize = 200;
        while (true) {
            Console.log("{}-------{}", pageNum, pageSize);
            Page<EmbyItemPic> picPage = embyItemPicDao.findAllByPage(pageNum, pageSize);
            List<EmbyItemPic> records = picPage.getRecords();

            List<List<EmbyItemPic>> partitions = CollUtil.partition(records, 100);
            for (List<EmbyItemPic> p : partitions) {
                List<String> itemIds = p.stream().map(i -> NumberUtil.toStr(i.getItemId())).toList();
                EmbyItemsInfoOut infoOut = embyProxy.getItemInfos(itemIds);
                if (infoOut.getTotalRecordCount() == p.size()) {
                    continue;
                }
                List<String> realItemIds = infoOut.getItems().stream().map(EmbyItem::getItemId).toList();
                List<String> removeItemIds = CollUtil.subtractToList(itemIds, realItemIds);
                Console.log(removeItemIds);
                removeCnt += embyItemPicDao.delById(removeItemIds.stream().map(NumberUtil::parseInt).toList());
            }

            if (pageNum >= picPage.getTotalPage()) {
                break;
            }
            pageNum++;
        }

        return Rsres.success(removeCnt);
    }

    // 清除缓存文件
    @GetMapping("/clear/file-cache")
    public Rsres<Object> clearFileCache() {
        int removeCnt = 0;
        int pageNum = 1;
        int pageSize = 200;
        while (true) {
            Page<EmbyItemPic> picPage = embyItemPicDao.findAllByPage(pageNum, pageSize);
            List<EmbyItemPic> records = picPage.getRecords();

            List<List<EmbyItemPic>> partitions = CollUtil.partition(records, 50);
            for (List<EmbyItemPic> p : partitions) {
                List<String> itemIds = p.stream().map(i -> NumberUtil.toStr(i.getItemId())).toList();
                EmbyItemsInfoOut infoOut = embyProxy.getItemInfos(itemIds);
                if (infoOut.getTotalRecordCount() == p.size()) {
                    continue;
                }
                List<String> realItemIds = infoOut.getItems().stream().map(EmbyItem::getItemId).toList();
                List<String> removeItemIds = CollUtil.subtractToList(itemIds, realItemIds);
                Console.log(CollUtil.subtractToList(itemIds, realItemIds));
                removeCnt += embyItemPicDao.delById(removeItemIds.stream().map(NumberUtil::parseInt).toList());
            }

            if (pageNum >= picPage.getTotalPage()) {
                break;
            }
            pageNum++;
        }

        return Rsres.success(removeCnt);
    }

    // 构建tmdb&豆瓣本地库
    @GetMapping("/build/tmdb-douban")
    public Rsres<Object> buildTmdbDouban(Integer min, Integer max) {
        String uniqueKey = "unique:tmdbId";
        // List<Integer> itemIds = embyItemPicDao.findAllItemId();
        List<Integer> allItemIds = ListUtil.of();
        if (null == min || null == max) {
            allItemIds = collectionTaskService.getAllItemIdByCache(null)
                    .stream().map(NumberUtil::parseInt).toList();
        } else {
            allItemIds = IntStream.rangeClosed(min, max).boxed().toList();
        }
        processItemsWith3Threads(allItemIds, uniqueKey);

        /*AtomicInteger i = new AtomicInteger();
        allItemIds.forEach(itemId -> {
            String value = redisClient.hgetStr(uniqueKey, itemId.toString());
            if (StrUtil.isNotBlank(value)) {
                System.out.print(".");
                return;
            }
            // ThreadUtil.safeSleep(RandomUtil.randomInt(50, 500));
            // ThreadUtil.execVirtual(() -> {
            try {
                // ThreadUtil.safeSleep(RandomUtil.randomInt(50, 500));
                List<EmbyItem> embyItems = embyProxy.getItemInfoByCache(itemId.toString());
                if (CollUtil.isEmpty(embyItems)) {
                    return;
                }
                EmbyItem embyItem = CollUtil.getFirst(embyItems);
                embyProxy.initTmdbProvider(embyItem);
                redisClient.hset(uniqueKey, itemId.toString(), "1");
                i.getAndIncrement();
            } catch (Exception e) {
                log.warn("Controller-[itemId:{}]构建tmdb-douban失败: ", itemId, e);
            } finally {
                ThreadUtil.safeSleep(RandomUtil.randomInt(10, 50));
            }
            // });
        });
        log.warn("构建tmdb&豆瓣本地库==>执行完成, 共处理: {}条", i.get());*/
        return Rsres.success("构建tmdb&豆瓣本地库==>执行中");
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


    // 构建tmdb&豆瓣本地库-补全豆瓣id
    @GetMapping("/build/completion-doubanId")
    public Rsres<Object> buildCompletionDoubanId() {
        String uniqueKey = "unique:imdbId-doubanId";
        List<TmdbProvider> tmdbProviders = tmdbProviderDao.findAllImdbNoDouBan();

        ThreadUtil.execAsync(() -> {
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
        });
        return Rsres.success("构建tmdb&豆瓣本地库(补全豆瓣id)==>执行中");
    }

    // 构建tmdb&豆瓣本地库-补全豆瓣json
    @GetMapping("/build/completion-doubanInfo")
    public Rsres<Object> buildCompletionDoubanInfo() {
        String uniqueKey = "unique:doubanInfo";
        List<TmdbProvider> tmdbProviders = tmdbProviderDao.findAllNoDouBanInfo();

        ThreadUtil.execAsync(() -> {
            AtomicInteger i = new AtomicInteger();
            tmdbProviders.forEach(tmdbProvider -> {
                String doubanId = tmdbProvider.getDoubanId();
                EmbyMediaType type = EmbyMediaType.fromEmby(tmdbProvider.getEmbyType());
                String value = redisClient.hgetStr(uniqueKey, doubanId);
                if (StrUtil.isNotBlank(value)) {
                    return;
                }
                try {
                    String doubanInfo = doubanProxy.getInfoById(type, doubanId);
                    if (StrUtil.isNotBlank(doubanInfo)) {
                        tmdbProvider.setDoubanInfo(doubanInfo);
                        tmdbProvider.setDoubanRate(JSONUtil.parseObj(doubanInfo)
                                .getJSONObject("rating").getBigDecimal("value"));
                        tmdbProvider.setUpdateTime(new DateTime());
                        tmdbProvider.setRateUpdateTime(new DateTime());
                        tmdbProviderDao.updateById(tmdbProvider);
                    }
                    redisClient.hset(uniqueKey, doubanId, "1");
                    i.getAndIncrement();
                    ThreadUtil.safeSleep(RandomUtil.randomInt(8_000, 24_000));
                } catch (Exception e) {
                    log.warn("Controller-[itemId:{}]构建tmdb-douban-补全豆瓣Info失败: ", doubanId, e);
                    ThreadUtil.safeSleep(120_000);
                }
            });
            log.warn("构建tmdb&豆瓣本地库(补全豆瓣json)==>执行完成, 共处理: {}条", i.get());
        });
        return Rsres.success("构建tmdb&豆瓣本地库(补全豆瓣json)==>执行中");
    }

}