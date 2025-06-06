package cn.acecandy.fasaxi.emma.control;

import cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType;
import cn.acecandy.fasaxi.emma.common.resp.Rsres;
import cn.acecandy.fasaxi.emma.dao.entity.EmbyItemPic;
import cn.acecandy.fasaxi.emma.dao.entity.TmdbProvider;
import cn.acecandy.fasaxi.emma.dao.service.EmbyItemPicDao;
import cn.acecandy.fasaxi.emma.dao.service.TmdbProviderDao;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.out.EmbyItem;
import cn.acecandy.fasaxi.emma.sao.out.EmbyItemsInfoOut;
import cn.acecandy.fasaxi.emma.sao.proxy.DoubanProxy;
import cn.acecandy.fasaxi.emma.sao.proxy.EmbyProxy;
import cn.acecandy.fasaxi.emma.utils.ThreadUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.date.DateTime;
import org.dromara.hutool.core.lang.Console;
import org.dromara.hutool.core.math.NumberUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.core.util.RandomUtil;
import org.dromara.hutool.json.JSONUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Slf4j
@Tag(name = "内部api")
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

    @Operation(summary = "当前系统时间")
    @GetMapping("/time")
    public Rsres<Object> health() {
        return Rsres.success(System.currentTimeMillis());
    }

    @Operation(summary = "清除db无用图片")
    @GetMapping("/clear/db-pic")
    public Rsres<Object> clearDbPic() {
        int removeCnt = 0;
        int pageNum = 1;
        int pageSize = 200;
        while (true) {
            IPage<EmbyItemPic> picPage = embyItemPicDao.findAllByPage(pageNum, pageSize);
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

            long totalPages = picPage.getPages();
            if (pageNum >= totalPages) {
                break;
            }
            pageNum++;
        }

        return Rsres.success(removeCnt);
    }

    @Operation(summary = "清除缓存文件")
    @GetMapping("/clear/file-cache")
    public Rsres<Object> clearFileCache() {
        int removeCnt = 0;
        int pageNum = 1;
        int pageSize = 200;
        while (true) {
            IPage<EmbyItemPic> picPage = embyItemPicDao.findAllByPage(pageNum, pageSize);
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

            long totalPages = picPage.getPages();
            if (pageNum >= totalPages) {
                break;
            }
            pageNum++;
        }

        return Rsres.success(removeCnt);
    }

    @Operation(summary = "构建tmdb&豆瓣本地库")
    @GetMapping("/build/tmdb-douban")
    public Rsres<Object> buildTmdbDouban() {
        String uniqueKey = "unique:tmdb-douban";
        // List<Integer> itemIds = embyItemPicDao.findAllItemId();
        ThreadUtil.execVirtual(() -> {
            AtomicInteger i = new AtomicInteger();
            IntStream.rangeClosed(10_000, 2_000_000).boxed().toList()
                    .parallelStream().forEach(itemId -> {
                        String value = redisClient.hgetStr(uniqueKey, itemId.toString());
                        if (StrUtil.isNotBlank(value)) {
                            return;
                        }
                        try {
                            EmbyItem embyItem = embyProxy.getItemInfoByCache(itemId.toString());
                            embyProxy.initTmdbProvider(embyItem);
                            redisClient.hset(uniqueKey, itemId.toString(), "1");
                            i.getAndIncrement();
                        } catch (Exception e) {
                            log.warn("Controller-[itemId:{}]构建tmdb-douban失败: ", itemId, e);
                        } finally {
                            ThreadUtil.safeSleep(RandomUtil.randomInt(400, 1400));
                        }
                    });
            log.warn("构建tmdb&豆瓣本地库==>执行完成, 共处理: {}条", i.get());
        });
        return Rsres.success("构建tmdb&豆瓣本地库==>执行中");
    }

    @Operation(summary = "构建tmdb&豆瓣本地库-补全豆瓣id")
    @GetMapping("/build/completion-doubanId")
    public Rsres<Object> buildCompletionDoubanId() {
        String uniqueKey = "unique:imdbId-doubanId";
        List<TmdbProvider> tmdbProviders = tmdbProviderDao.findAllImdbNoDouBan();

        ThreadUtil.execVirtual(() -> {
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

    @Operation(summary = "构建tmdb&豆瓣本地库-补全豆瓣json")
    @GetMapping("/build/completion-doubanInfo")
    public Rsres<Object> buildCompletionDoubanInfo() {
        String uniqueKey = "unique:doubanInfo";
        List<TmdbProvider> tmdbProviders = tmdbProviderDao.findAllNoDouBanInfo();

        ThreadUtil.execVirtual(() -> {
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
                } catch (Exception e) {
                    log.warn("Controller-[itemId:{}]构建tmdb-douban-补全豆瓣Info失败: ", doubanId, e);
                } finally {
                    ThreadUtil.safeSleep(RandomUtil.randomInt(30_000, 6_000));
                }
            });
            log.warn("构建tmdb&豆瓣本地库(补全豆瓣json)==>执行完成, 共处理: {}条", i.get());
        });
        return Rsres.success("构建tmdb&豆瓣本地库(补全豆瓣json)==>执行中");
    }

}