package cn.acecandy.fasaxi.emma.task.impl;

import cn.acecandy.fasaxi.emma.common.enums.StrmPathPrefix;
import cn.acecandy.fasaxi.emma.config.OpConfig;
import cn.acecandy.fasaxi.emma.dao.embyboss.entity.VideoPathRelation;
import cn.acecandy.fasaxi.emma.dao.embyboss.service.VideoPathRelationDao;
import cn.acecandy.fasaxi.emma.sao.proxy.EmbyProxy;
import cn.acecandy.fasaxi.emma.sao.proxy.OpProxy;
import cn.acecandy.fasaxi.emma.utils.CloudUtil;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.io.file.FileNameUtil;
import cn.hutool.v7.core.lang.Console;
import cn.hutool.v7.core.lang.mutable.MutableTriple;
import cn.hutool.v7.core.text.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

import static cn.acecandy.fasaxi.emma.common.enums.CloudStorageType.R_115;
import static cn.acecandy.fasaxi.emma.common.enums.CloudStorageType.R_123;
import static cn.acecandy.fasaxi.emma.common.enums.StrmPathPrefix.PRE_NEW115;
import static cn.acecandy.fasaxi.emma.common.enums.StrmPathPrefix.PRE_ZONG123;
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

    @Resource
    private EmbyProxy embyProxy;

    private static final String COMMON_UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36 Edg/142.0.0.0";

    private static final String PREFIX_MICU = "/pt/";
    private static final String PREFIX_NEW115_MICU = "/new115/worldline/";
    private static final String PREFIX_NEW115 = "/new115/";
    private static final String PREFIX_ZONG123 = "/zong123/";

    public void uploadTo115() {
        List<VideoPathRelation> relations = videoPathRelationDao.findNoBak(
                StrmPathPrefix.PRE_MICU.getType(), null);
        processVideoRelations(relations, PRE_NEW115, this::handleBakStatus0, this::handleBakStatus1);
    }

    public void r115to123() {
        List<VideoPathRelation> relations = videoPathRelationDao.findNoBak123(null);
        processVideoRelations123(relations, PRE_ZONG123, this::handleBak123Status0, this::handleBak123Status1);
    }

    private void processVideoRelations(List<VideoPathRelation> relations,
                                       StrmPathPrefix prefix,
                                       StatusHandler status0Handler, StatusHandler status1Handler) {
        if (CollUtil.isEmpty(relations)) {
            return;
        }
        relations.forEach(v -> {
            try {
                if (v.getBakStatus() == 0) {
                    status0Handler.handle(v);
                } else if (v.getBakStatus() == 1) {
                    status1Handler.handle(v);
                }
            } catch (Exception e) {
                log.warn("[关联关系处理-备份至{}] 失败:itemId={}, purePath={}, error={}",
                        prefix.getType(), v.getItemId(), v.getPurePath(), e.getMessage(), e);
            }
        });
    }

    private void processVideoRelations123(List<VideoPathRelation> relations, StrmPathPrefix prefix,
                                          StatusHandler status0Handler, StatusHandler status1Handler) {
        if (CollUtil.isEmpty(relations)) {
            return;
        }
        relations.forEach(v -> {
            try {
                if (v.getBakStatus123() == 0) {
                    status0Handler.handle(v);
                } else if (v.getBakStatus123() == 1) {
                    status1Handler.handle(v);
                }
            } catch (Exception e) {
                log.warn("[关联关系处理-备份至{}] 失败:itemId={}, purePath={}, error={}",
                        prefix.getType(), v.getItemId(), v.getPurePath(), e.getMessage(), e);
            }
        });
    }

    @FunctionalInterface
    private interface StatusHandler {
        /**
         * 备份处理
         *
         * @param relation 关系
         * @throws Exception 例外
         */
        void handle(VideoPathRelation relation) throws Exception;
    }

    private void handleBakStatus0(VideoPathRelation v) {
        String purePath = v.getPurePath();
        String fileName = FileNameUtil.getName(purePath);
        String purePathDir = StrUtil.removeSuffix(purePath, SLASH + fileName);
        String sourcePathDir = StrUtil.format(PREFIX_MICU + "{}", purePathDir);
        String targetPathDir = StrUtil.format(PREFIX_NEW115_MICU + "{}", purePathDir);

        if (opProxy.mkdir(targetPathDir)) {
            if (opProxy.copy(sourcePathDir, targetPathDir, Collections.singletonList(fileName))) {
                videoPathRelationDao.updateByItemId(
                        VideoPathRelation.x().setItemId(v.getItemId()).setBakStatus(1));
            }
        }
    }

    private void handleBakStatus1(VideoPathRelation v) {
        String purePath = v.getPurePath();
        String targetPathFull = StrUtil.format(PREFIX_NEW115_MICU + "{}", purePath);
        String cloudPath = opConfig.getDHost() + targetPathFull;

        String real302Url = cloudUtil.redirect302ByOpenlist(R_115, cloudPath, COMMON_UA);
        if (StrUtil.isNotBlank(real302Url)) {
            videoPathRelationDao.updateByItemId(VideoPathRelation.x()
                    .setItemId(v.getItemId()).setBakStatus(2).setPath115(cloudPath));
        }
    }

    private void handleBak123Status0(VideoPathRelation v) {
        String path115 = v.getPath115();
        MutableTriple<String, StrmPathPrefix, String> splitResult = StrmPathPrefix.split(path115);
        String purePath = splitResult.getRight();

        String fileName = FileNameUtil.getName(purePath);
        String purePathDir = StrUtil.removeSuffix(purePath, SLASH + fileName);

        embyProxy.trans115To123(purePathDir, purePath);
        videoPathRelationDao.updateByItemId(
                VideoPathRelation.x().setItemId(v.getItemId()).setBakStatus123(1));
    }

    private void handleBak123Status1(VideoPathRelation v) {
        String path115 = v.getPath115();
        MutableTriple<String, StrmPathPrefix, String> splitResult = StrmPathPrefix.split(path115);
        String purePath = splitResult.getRight();
        String targetPathFull = PRE_ZONG123.getValue() + purePath;
        String cloudPath = opConfig.getHost() + targetPathFull;

        String real302Url = cloudUtil.redirect302ByOpenlist(R_123, cloudPath, COMMON_UA);
        if (StrUtil.isNotBlank(real302Url)) {
            videoPathRelationDao.updateByItemId(
                    VideoPathRelation.x().setItemId(v.getItemId()).setBakStatus123(2)
                            .setPath123(cloudPath));
        }
    }
}