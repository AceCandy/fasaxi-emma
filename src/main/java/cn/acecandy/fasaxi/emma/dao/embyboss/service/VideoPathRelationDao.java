package cn.acecandy.fasaxi.emma.dao.embyboss.service;

import cn.acecandy.fasaxi.emma.dao.embyboss.entity.VideoPathRelation;
import cn.acecandy.fasaxi.emma.dao.embyboss.mapper.VideoPathRelationMapper;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.date.DateUtil;
import cn.hutool.v7.core.lang.Console;
import cn.hutool.v7.core.text.StrUtil;
import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import static cn.acecandy.fasaxi.emma.dao.embyboss.entity.table.VideoPathRelationTableDef.VIDEO_PATH_RELATION;

/**
 * 视频路径关联 dao
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@Slf4j
@UseDataSource("embyboss")
@Component
public class VideoPathRelationDao extends ServiceImpl<VideoPathRelationMapper, VideoPathRelation> {

    /**
     * 插入或更新
     * <p>
     * 主要靠有无二次传名称来判断 更新的时候不传
     *
     * @param dto 到
     * @return boolean
     */
    public boolean insertOrUpdate(VideoPathRelation dto) {
        if (dto == null) {
            return false;
        }
        if (StrUtil.isBlank(dto.getItemName())) {
            return mapper.update(dto) > 0;
        } else {
            return mapper.insertSelective(dto) > 0;
        }
    }

    /**
     * 按id更新
     * <p>
     * 必须有itemId
     *
     * @param dto 数据传输对象
     * @return boolean
     */
    public boolean updateByItemId(VideoPathRelation dto) {
        if (null == dto.getItemId()) {
            return false;
        }
        return mapper.update(dto) > 0;
    }

    /**
     * 按id更新
     * <p>
     * 必须有itemId
     *
     * @param dto 数据传输对象
     * @return boolean
     */
    public boolean updateCheckTimeByItemId(Collection<? extends Serializable> ids) {
        if (CollUtil.isEmpty(ids)) {
            return false;
        }
        VideoPathRelation dto = VideoPathRelation.x().setCheckTime(DateUtil.now());
        QueryWrapper wrapper = QueryWrapper.create()
                .where(VIDEO_PATH_RELATION.ITEM_ID.in(ids));
        return mapper.updateByQuery(dto, wrapper) > 0;
    }

    public boolean delByItemIds(Collection<? extends Serializable> ids) {
        if (CollUtil.isEmpty(ids)) {
            return false;
        }
        return mapper.deleteByCondition(VIDEO_PATH_RELATION.ITEM_ID.in(ids)) > 0;
    }


    public void update() {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(VIDEO_PATH_RELATION.PATH123.like("\\%2F"));
        List<VideoPathRelation> relations = mapper.selectListByQuery(wrapper);
        if (CollUtil.isEmpty(relations) || CollUtil.size(relations) != 12) {
            return;
        }
        relations.forEach(v -> {
            VideoPathRelation vv = VideoPathRelation.x().setItemId(v.getItemId())
                    .setPath123("http://192.168.1.249:5244" + v.getPathPrefix() + v.getPurePath());
            updateByItemId(vv);
        });
    }

    /**
     * 按ID查找
     *
     * @param id ID
     * @return {@link VideoPathRelation }
     */
    public VideoPathRelation findById(Integer id) {
        if (null == id) {
            return null;
        }
        QueryWrapper wrapper = QueryWrapper.create()
                .where(VIDEO_PATH_RELATION.ITEM_ID.eq(id));
        return mapper.selectOneByQuery(wrapper);
    }

    /**
     * 查找对应类型未备份数据
     *
     * @param strmType 路径类型
     * @return {@link VideoPathRelation }
     */
    public List<VideoPathRelation> findNoBak(String strmType, List<Integer> status) {
        if (StrUtil.isBlank(strmType)) {
            return null;
        }
        if (CollUtil.isEmpty(status)) {
            status = List.of(0, 1);
        }
        QueryWrapper wrapper = QueryWrapper.create()
                .where(VIDEO_PATH_RELATION.STRM_TYPE.eq(strmType))
                .and(VIDEO_PATH_RELATION.BAK_STATUS.in(status))
                .limit(100);
        return mapper.selectListByQuery(wrapper);
    }

    /**
     * 查找对应类型未备份数据
     *
     * @return {@link VideoPathRelation }
     */
    public List<VideoPathRelation> findNoBak123(List<Integer> status) {
        if (CollUtil.isEmpty(status)) {
            status = List.of(0, 1);
        }
        QueryWrapper wrapper = QueryWrapper.create()
                .where(VIDEO_PATH_RELATION.BAK_STATUS.in(status))
                .and(VIDEO_PATH_RELATION.PATH115.isNotNull())
                .and(VIDEO_PATH_RELATION.PATH123.isNull())
                .or(VIDEO_PATH_RELATION.PATH123.eq(""))
                .limit(20);
        Console.log(mapper.selectListByQuery(wrapper));
        return mapper.selectListByQuery(wrapper);
    }

    /**
     * 查找需要检查的数据
     *
     * @return {@link VideoPathRelation }
     */
    public List<VideoPathRelation> findNeedCheck() {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(VIDEO_PATH_RELATION.CHECK_TIME.isNull())
                .or(VIDEO_PATH_RELATION.CHECK_TIME.le(DateUtil.lastWeek()))
                .or(VIDEO_PATH_RELATION.BAK_STATUS.eq(1))
                .or(VIDEO_PATH_RELATION.BAK_STATUS123.eq(1))
                .limit(100);
        Console.log(mapper.selectListByQuery(wrapper));
        return mapper.selectListByQuery(wrapper);
    }
}