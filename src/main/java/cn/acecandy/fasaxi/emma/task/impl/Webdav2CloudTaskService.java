package cn.acecandy.fasaxi.emma.task.impl;

import cn.acecandy.fasaxi.emma.common.enums.StrmPathPrefix;
import cn.acecandy.fasaxi.emma.config.OpConfig;
import cn.acecandy.fasaxi.emma.dao.embyboss.entity.VideoPathRelation;
import cn.acecandy.fasaxi.emma.dao.embyboss.service.VideoPathRelationDao;
import cn.acecandy.fasaxi.emma.sao.proxy.OpProxy;
import cn.acecandy.fasaxi.emma.utils.CloudUtil;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.io.file.FileNameUtil;
import cn.hutool.v7.core.text.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

import static cn.acecandy.fasaxi.emma.common.enums.CloudStorageType.R_115;
import static cn.acecandy.fasaxi.emma.common.enums.CloudStorageType.R_123;
import static cn.hutool.v7.core.text.StrPool.SLASH;

/**
 * webdav资源上传到云 实现
 *
 * @author tangningzhu
 * @since 2025/3/3
 */
@Slf4j
@Component
public class Webdav2CloudTaskService {

    @Resource
    private VideoPathRelationDao videoPathRelationDao;

    @Resource
    private OpProxy opProxy;

    @Resource
    private OpConfig opConfig;

    @Resource
    private CloudUtil cloudUtil;

    /**
     * 通用ua
     */
    private static final String COMMON_UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36 Edg/142.0.0.0";

    /**
     * 通用ua
     */
    private static final String PREFIX_MICU = "/pt/";
    private static final String PREFIX_NEW115 = "/new115/worldline/";
    private static final String PREFIX_ZONG123 = "/zong123/worldline/";

    public void uploadTo115() {
        List<VideoPathRelation> videoPathRelations = videoPathRelationDao
                .findNoBak(StrmPathPrefix.PRE_MICU.getType());
        if (CollUtil.isEmpty(videoPathRelations)) {
            return;
        }
        videoPathRelations.forEach(v -> {
            try {
                if (v.getBakStatus() == 0) {
                    handleBakStatus0(v);
                } else if (v.getBakStatus() == 1) {
                    handleBakStatus1(v);
                }
            } catch (Exception e) {
                log.warn("关联关系处理上传到115失败:{}", v.getItemId(), e);
            }
        });
    }

    public void r115to123() {
        List<VideoPathRelation> videoPathRelations = videoPathRelationDao.findNoBak123();
        if (CollUtil.isEmpty(videoPathRelations)) {
            return;
        }
        videoPathRelations.forEach(v -> {
            try {
                if (v.getBakStatus() == 0) {
                    handleBak123Status0(v);
                } else if (v.getBakStatus() == 1) {
                    handleBak123Status1(v);
                }
            } catch (Exception e) {
                log.warn("关联关系处理上传到123失败:{}", v.getItemId(), e);
            }
        });
    }

    private void handleBakStatus0(VideoPathRelation v) {
        String purePath = v.getPurePath();
        String fileName = FileNameUtil.getName(purePath);
        String purePathDir = StrUtil.removeSuffix(purePath, SLASH + fileName);
        String sourcePathDir = StrUtil.format(PREFIX_MICU + "{}", purePathDir);

        String targetPathDir = StrUtil.format(PREFIX_NEW115 + "{}", purePathDir);
        if (opProxy.mkdir(targetPathDir)) {
            opProxy.copy(sourcePathDir, targetPathDir, Collections.singletonList(fileName));
            videoPathRelationDao.updateByItemId(VideoPathRelation.x()
                    .setItemId(v.getItemId()).setBakStatus(1));
        }
    }

    private void handleBakStatus1(VideoPathRelation v) {
        String purePath = v.getPurePath();
        String targetPathFull = StrUtil.format(PREFIX_NEW115 + "{}", purePath);

        String cloudPath = opConfig.getDHost() + targetPathFull;
        String real302Url = cloudUtil.redirect302ByOpenlist(R_115, cloudPath, COMMON_UA);
        if (StrUtil.isNotBlank(real302Url)) {
            // 更新
            videoPathRelationDao.updateByItemId(VideoPathRelation.x()
                    .setItemId(v.getItemId()).setBakStatus(2)
                    .setPath115(cloudPath));
        }
    }

    private void handleBak123Status0(VideoPathRelation v) {
        String purePath = v.getPurePath();
        String fileName = FileNameUtil.getName(purePath);
        String purePathDir = StrUtil.removeSuffix(purePath, SLASH + fileName);
        String sourcePathDir = StrUtil.format(PREFIX_NEW115 + "{}", purePathDir);

        String targetPathDir = StrUtil.format(PREFIX_ZONG123 + "{}", purePathDir);
        if (opProxy.mkdir(targetPathDir)) {
            opProxy.copy(sourcePathDir, targetPathDir, Collections.singletonList(fileName));
            videoPathRelationDao.updateByItemId(VideoPathRelation.x()
                    .setItemId(v.getItemId()).setBakStatus123(1));
        }
    }

    private void handleBak123Status1(VideoPathRelation v) {
        String purePath = v.getPurePath();
        String targetPathFull = StrUtil.format(PREFIX_ZONG123 + "{}", purePath);

        String cloudPath = opConfig.getDHost() + targetPathFull;
        String real302Url = cloudUtil.redirect302ByOpenlist(R_123, cloudPath, COMMON_UA);
        if (StrUtil.isNotBlank(real302Url)) {
            // 更新
            videoPathRelationDao.updateByItemId(VideoPathRelation.x()
                    .setItemId(v.getItemId()).setBakStatus123(2)
                    .setPath115(cloudPath));
        }
    }

}