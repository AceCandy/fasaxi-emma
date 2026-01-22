package cn.acecandy.fasaxi.emma.task.impl;

import cn.acecandy.fasaxi.emma.dao.embyboss.entity.SdItem;
import cn.acecandy.fasaxi.emma.dao.embyboss.service.SdItemDao;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.out.DataEyeItem;
import cn.acecandy.fasaxi.emma.sao.proxy.DataEyeProxy;
import cn.acecandy.fasaxi.emma.sao.proxy.OpProxy;
import cn.hutool.v7.core.bean.BeanUtil;
import cn.hutool.v7.core.date.DateUtil;
import cn.hutool.v7.core.io.file.FileNameUtil;
import cn.hutool.v7.core.io.file.FileUtil;
import cn.hutool.v7.core.net.url.UrlUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.http.client.HttpDownloader;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

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
    private RedisClient redisClient;

    @Resource
    private OpProxy opProxy;

    private static final Integer SOURCE_剧查查 = 1;

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
        Long maxSourceId = sdItemDao.getMaxSourceId(1);
        if (redisClient.hasKey(CACHE_ID_KEY)) {
            maxSourceId = (Long) redisClient.get(CACHE_ID_KEY);
        }

        long nextSourceId = maxSourceId + 1;
        int nullCount = 0;
        while (true) {
            DataEyeItem item = dataEyeProxy.getItemInfo(nextSourceId);
            if (item == null) {
                nullCount++;
                if (nullCount >= 100) {
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

            sdItemDao.save(sdItem);
            nextSourceId = item.getPlayletId() + 1;
        }
        redisClient.set(CACHE_ID_KEY, nextSourceId);
    }

}