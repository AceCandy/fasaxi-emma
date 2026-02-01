package cn.acecandy.fasaxi.emma.dao.embyboss.service;

import cn.acecandy.fasaxi.emma.dao.embyboss.entity.JavRank;
import cn.acecandy.fasaxi.emma.dao.embyboss.entity.SdItem;
import cn.acecandy.fasaxi.emma.dao.embyboss.entity.table.JavRankTableDef;
import cn.acecandy.fasaxi.emma.dao.embyboss.mapper.JavRankMapper;
import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

import static cn.acecandy.fasaxi.emma.dao.embyboss.entity.table.JavRankTableDef.JAV_RANK;
import static com.mybatisflex.core.query.QueryMethods.max;

/**
 * jav榜单 dao
 *
 * @author AceCandy
 * @since 2026-01-18
 */
@Slf4j
@UseDataSource("embyboss")
@Component
public class JavRankDao extends ServiceImpl<JavRankMapper, JavRank> {


    /**
     * 按类型查找最近日期
     *
     * @param rankType 等级类型
     * @return {@link JavRank }
     */
    public JavRank findByTypeOnMaxDate(String rankType) {
        return mapper.selectOneByQuery(
                query().where(JAV_RANK.RANK_DATE.eq(
                        query().select(max(JAV_RANK.RANK_DATE))
                                .where(JAV_RANK.RANK_TYPE.eq(rankType)))
                ));
    }

    /**
     * 按类型查找最近日期
     *
     * @param rankType 等级类型
     * @return {@link JavRank }
     */
    public JavRank findByTypeAndDate(String rankType, String rankDate) {
        return mapper.selectOneByQuery(
                query().where(JAV_RANK.RANK_TYPE.eq(rankType))
                        .and(JAV_RANK.RANK_DATE.eq(rankDate)));
    }
}