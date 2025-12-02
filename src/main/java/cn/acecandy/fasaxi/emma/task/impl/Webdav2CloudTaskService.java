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
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.core.text.split.SplitUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

import static cn.acecandy.fasaxi.emma.common.enums.CloudStorageType.L_MICU;
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
    private EmbyProxy embyProxy;

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

    public void uploadTo115() {
        List<VideoPathRelation> videoPathRelations = videoPathRelationDao
                .findNoBak(StrmPathPrefix.PRE_MICU.getType());
        if (CollUtil.isEmpty(videoPathRelations)) {
            return;
        }
        videoPathRelations.forEach(v -> {
            try {
                // 先检查备份状态，避免重复处理
                if (v.getBakStatus() == 1) {
                    // 如果是备份中就无需重复处理了
                    return;
                }
                
                String purePath = v.getPurePath();
                String fileName = FileNameUtil.getName(purePath);
                // 使用更安全的方式获取目录路径
                int lastSlashIndex = purePath.lastIndexOf(SLASH);
                String purePathDir = lastSlashIndex > 0 ? purePath.substring(0, lastSlashIndex) : "";
                String sourcePathDir = StrUtil.format("/pt/{}", purePathDir);
                String sourcePathFull = StrUtil.format("/pt/{}", purePath);

                String targetPathDir = StrUtil.format("/new115/worldline/{}", purePathDir);
                String targetPathFull = StrUtil.format("/new115/worldline/{}", purePath);

                String r115MediaPath = opConfig.getDHost() + targetPathFull;
                String real302Url = cloudUtil.redirect302ByOpenlist(L_MICU, r115MediaPath, COMMON_UA);
                if (StrUtil.isNotBlank(real302Url)) {
                    // 更新
                    videoPathRelationDao.updateByItemId(VideoPathRelation.x()
                            .setItemId(v.getItemId()).setBakStatus(2)
                            .setPath115(r115MediaPath));
                    return;
                }

                if (opProxy.mkdir(targetPathDir)) { // 先创建目录
                    // 源文件目录（从原始路径中移除文件名）
                    String parentDir = CollUtil.getLast(SplitUtil.splitPath(sourcePathDir));
                    String parentSourceDir = StrUtil.removeSuffix(sourcePathDir, parentDir);
                    String parentTargetDir = StrUtil.removeSuffix(targetPathDir, parentDir);
                    opProxy.copy(parentSourceDir, parentTargetDir, Collections.singletonList(parentDir));
                    videoPathRelationDao.updateByItemId(VideoPathRelation.x()
                            .setItemId(v.getItemId()).setBakStatus(1));
                }
            } catch (Exception e) {
                log.warn("关联关系处理上传到115失败:{}", v.getItemId(), e);
                // 可以考虑添加重试逻辑或状态更新
                try {
                    videoPathRelationDao.updateByItemId(VideoPathRelation.x()
                            .setItemId(v.getItemId()).setBakStatus(0)); // 重置为未备份状态
                } catch (Exception updateEx) {
                    log.error("更新备份状态失败:{}", v.getItemId(), updateEx);
                }
            }
        });
    }

}