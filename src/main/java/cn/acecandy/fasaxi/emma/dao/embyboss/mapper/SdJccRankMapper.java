package cn.acecandy.fasaxi.emma.dao.embyboss.mapper;

import cn.acecandy.fasaxi.emma.dao.embyboss.entity.SdJccRank;
import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 剧查查-短剧榜单表 mapper
 *
 * @author AceCandy
 * @since 2026-01-23
 */
@UseDataSource("embyboss")
@Mapper
public interface SdJccRankMapper extends BaseMapper<SdJccRank> {
}
