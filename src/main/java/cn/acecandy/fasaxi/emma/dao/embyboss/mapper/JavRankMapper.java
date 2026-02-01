package cn.acecandy.fasaxi.emma.dao.embyboss.mapper;

import cn.acecandy.fasaxi.emma.dao.embyboss.entity.JavRank;
import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * jav榜单 mapper
 *
 * @author AceCandy
 * @since 2026-01-18
 */
@UseDataSource("embyboss")
@Mapper
public interface JavRankMapper extends BaseMapper<JavRank> {
}