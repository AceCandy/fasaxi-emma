package cn.acecandy.fasaxi.emma.dao.embyboss.service;

import cn.acecandy.fasaxi.emma.dao.embyboss.entity.SdJccRank;
import cn.acecandy.fasaxi.emma.dao.embyboss.mapper.SdJccRankMapper;
import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

import static cn.acecandy.fasaxi.emma.dao.embyboss.entity.table.SdJccRankTableDef.SD_JCC_RANK;

/**
 * 剧查查-短剧榜单表 dao
 *
 * @author AceCandy
 * @since 2026-01-23
 */
@Slf4j
@UseDataSource("embyboss")
@Component
public class SdJccRankDao extends ServiceImpl<SdJccRankMapper, SdJccRank> {

    /**
     * 按类型和时间段查找
     *
     * @param type      类型
     * @param timeStage 时间阶段
     * @return {@link List }<{@link SdJccRank }>
     */
    public List<SdJccRank> findByTypeAndTimeStage(Integer type, String timeStage) {
        return mapper.selectListByQuery(query().where(SD_JCC_RANK.TYPE.eq(type)
                .and(SD_JCC_RANK.TIME_STAGE.eq(timeStage))));
    }

    /**
     * 按类型和时间段+值查找
     *
     * @param type      类型
     * @param timeStage 时间阶段
     * @return {@link List }<{@link SdJccRank }>
     */
    public SdJccRank findByTypeAndTimeStage(Integer type, String timeStage, String timeValue) {
        return mapper.selectOneByQuery(query().where(SD_JCC_RANK.TYPE.eq(type)
                .and(SD_JCC_RANK.TIME_STAGE.eq(timeStage))
                .and(SD_JCC_RANK.TIME_VALUE.eq(timeValue))));
    }

    /**
     * 按类型和时间段+like值查找
     *
     * @param type      类型
     * @param timeStage 时间阶段
     * @return {@link List }<{@link SdJccRank }>
     */
    public List<SdJccRank> findByTypeAndTimeStageLike(Integer type, String timeStage, String timeValue) {
        return mapper.selectListByQuery(query().where(SD_JCC_RANK.TYPE.eq(type)
                .and(SD_JCC_RANK.TIME_STAGE.eq(timeStage))
                .and(SD_JCC_RANK.TIME_VALUE.likeLeft(timeValue)))
                .orderBy(SD_JCC_RANK.TIME_VALUE.asc())
        );
    }
}