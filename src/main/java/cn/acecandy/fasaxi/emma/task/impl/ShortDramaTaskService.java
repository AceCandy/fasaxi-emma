package cn.acecandy.fasaxi.emma.task.impl;

import cn.acecandy.fasaxi.emma.dao.embyboss.entity.SdItem;
import cn.acecandy.fasaxi.emma.dao.embyboss.entity.SdJccRank;
import cn.acecandy.fasaxi.emma.dao.embyboss.service.SdItemDao;
import cn.acecandy.fasaxi.emma.dao.embyboss.service.SdJccRankDao;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.out.DataEyeItem;
import cn.acecandy.fasaxi.emma.sao.out.DataEyeRank;
import cn.acecandy.fasaxi.emma.sao.proxy.DataEyeProxy;
import cn.acecandy.fasaxi.emma.sao.proxy.OpProxy;
import cn.acecandy.fasaxi.emma.utils.DateUtil;
import cn.hutool.v7.core.bean.BeanUtil;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.io.file.FileNameUtil;
import cn.hutool.v7.core.io.file.FileUtil;
import cn.hutool.v7.core.lang.Console;
import cn.hutool.v7.core.net.url.UrlUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.http.client.HttpDownloader;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static cn.acecandy.fasaxi.emma.utils.DateUtil.时间阶段;
import static cn.acecandy.fasaxi.emma.utils.DateUtil.时间阶段_日;
import static cn.acecandy.fasaxi.emma.utils.DateUtil.时间阶段_月;


/**
 * 短剧任务相关 实现
 *
 * @author tangningzhu
 * @since 2025/3/3
 */
@Slf4j
@Component
public class ShortDramaTaskService {

    @Resource
    private DataEyeProxy dataEyeProxy;

    @Resource
    private SdItemDao sdItemDao;

    @Resource
    private SdJccRankDao sdJccRankDao;

    @Resource
    private RedisClient redisClient;

    @Resource
    private OpProxy opProxy;

    private static final Integer SOURCE_剧查查 = 1;

    private static final Integer 热力榜 = 1;
    private static final Integer 抖音热播 = 2;
    private static final Integer 快手热播 = 3;
    private static final Integer 红果热播 = 4;
    private static final Integer 品牌热播 = 5;
    private static final Integer 爱奇艺热播 = 11;
    private static final Integer 优酷热播 = 12;
    private static final Integer 腾讯竖版剧 = 13;
    private static final Integer 腾讯横版剧 = 14;
    private Map<Integer, Function<String, List<DataEyeRank>>> 榜单列表;

    @PostConstruct
    public void init() {
        榜单列表 = Map.of(
                热力榜, dataEyeProxy::getHotRank,
                抖音热播, dataEyeProxy::getDyRank,
                快手热播, dataEyeProxy::getKsRank,
                红果热播, dataEyeProxy::getHgRank,
                品牌热播, dataEyeProxy::getBrandRank,
                爱奇艺热播, dataEyeProxy::getIqyRank,
                优酷热播, dataEyeProxy::getYkRank,
                腾讯竖版剧, dataEyeProxy::getTxsRank,
                腾讯横版剧, dataEyeProxy::getTxhRank
        );
    }

    @Value("${dataeye.file-save-path-remote}")
    private String fileSavePathRemote;

    @Value("${dataeye.file-save-path}")
    private String fileSavePath;

    public static final String CACHE_ID_KEY = "SD_LAST_SYNC_ITEM_ID";

    /**
     * 同步项目信息（从当前的source_id的最大值+1开始）
     *
     */
    public void syncItemInfo() {
        Long maxSourceId = sdItemDao.getMaxSourceId(SOURCE_剧查查);
        /*if (redisClient.hasKey(CACHE_ID_KEY)) {
            long maxSourceIdRedis = ((Number) redisClient.get(CACHE_ID_KEY)).longValue();
            maxSourceId = Math.max(maxSourceId, maxSourceIdRedis);
        }*/

        long nextSourceId = maxSourceId + 1;
        int nullCount = 0;
        while (true) {
            DataEyeItem item = dataEyeProxy.getItemInfo(nextSourceId);
            if (item == null) {
                redisClient.set(CACHE_ID_KEY, nextSourceId);
                nullCount++;
                if (nullCount >= 500) {
                    break;
                }
                nextSourceId++;
                continue;
            }
            nullCount = 0;
            SdItem sdItem = BeanUtil.copyProperties(item, SdItem.class);
            sdItem.setSource(SOURCE_剧查查);
            sdItem.setSourceId(item.getPlayletId());
            sdItem.setTitle(item.getPlayletName());
            sdItem.setTags(item.getPlayletTagNames());

            if (StrUtil.isNotEmpty(item.getCoverOss())) {
                String fileName = StrUtil.format("{}-{}.{}", SOURCE_剧查查, item.getPlayletId(),
                        FileNameUtil.extName(UrlUtil.getPath(item.getCoverOss())));
                if (FileUtil.exists(fileSavePath)) {
                    if (!FileUtil.exists(fileSavePath + fileName)) {
                        HttpDownloader.of(item.getCoverOss()).downloadFile(
                                FileUtil.file(fileSavePath + fileName));
                    }
                } else {
                    String tmpFilePath = StrUtil.format("/tmp/{}/{}", DateUtil.formatToday(), fileName);
                    if (!FileUtil.exists(tmpFilePath)) {
                        File ossFile = HttpDownloader.of(item.getCoverOss()).downloadFile(
                                FileUtil.file(tmpFilePath));
                        opProxy.put(fileSavePathRemote + fileName, false, ossFile.getPath());
                    }
                }
                sdItem.setCoverUrl(fileName);
            }

            sdItemDao.save(sdItem);
            redisClient.set(CACHE_ID_KEY, nextSourceId);
            nextSourceId = item.getPlayletId() + 1;
        }
        log.warn("同步{}个短剧项目信息", nextSourceId - maxSourceId);
    }

    public void syncJccRanks() {
        榜单列表.forEach((b, method) -> {
            时间阶段.forEach(t -> {
                // initJccRank(b, t, method);
                fillOneDay(b, t, method);
            });
        });
    }

    /**
     * 初始化一次
     *
     * @param b      榜单 热力榜/抖音热播/快手热播/红果热播/品牌热播/爱奇艺热播/优酷热播/腾讯竖版剧/腾讯横版剧
     * @param t      时间阶段 天/月
     * @param method 方法
     */
    private void initJccRank(Integer b, String t, Function<String, List<DataEyeRank>> method) {
        List<SdJccRank> ranks = sdJccRankDao.findByTypeAndTimeStage(b, t);
        if (CollUtil.isNotEmpty(ranks)) {
            return;
        }
        String lastTimeStage = DateUtil.getLastTimeStage(t, null);
        while (true) {
            List<DataEyeRank> dataEyeRanks = method.apply(lastTimeStage);
            if (CollUtil.isEmpty(dataEyeRanks)) {
                break;
            }
            List<Long> rankList = dataEyeRanks.stream().map(DataEyeRank::getPlayletId).toList();
            SdJccRank.x().setType(b).setTimeStage(t).setTimeValue(lastTimeStage)
                    .setRankList(rankList).save();
            lastTimeStage = DateUtil.getLastTimeStage(t, lastTimeStage);
        }
    }

    /**
     * 补充最近一次(天/月)的数据
     *
     * @param b      b
     * @param t      t
     * @param method 方法
     */
    private void fillOneDay(Integer b, String t, Function<String, List<DataEyeRank>> method) {
        String lastTimeStage = DateUtil.getLastTimeStage(t, null);
        SdJccRank lastRank = sdJccRankDao.findByTypeAndTimeStage(b, t, lastTimeStage);
        if (null != lastRank) {
            return;
        }
        List<DataEyeRank> dataEyeRanks = method.apply(lastTimeStage);
        if (CollUtil.isEmpty(dataEyeRanks)) {
            return;
        }
        List<Long> rankList = dataEyeRanks.stream().map(DataEyeRank::getPlayletId).toList();
        SdJccRank.x().setType(b).setTimeStage(t).setTimeValue(lastTimeStage)
                .setRankList(rankList).save();
        log.warn("补充榜单[{}]-[{}]的数据", b, lastTimeStage);
    }


    static void main() {
        Console.log(DateUtil.getLastTimeStage(时间阶段_月, null));
        Console.log(DateUtil.getLastTimeStage(时间阶段_日, null));
        Console.log(DateUtil.getLastTimeStage(时间阶段_月, "2024-10-01"));
        Console.log(DateUtil.getLastTimeStage(时间阶段_日, "2024-10"));
    }
}