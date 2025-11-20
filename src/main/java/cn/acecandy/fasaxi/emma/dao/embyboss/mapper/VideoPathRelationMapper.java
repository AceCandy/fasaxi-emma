package cn.acecandy.fasaxi.emma.dao.embyboss.mapper;

import cn.acecandy.fasaxi.emma.dao.embyboss.entity.VideoPathRelation;
import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 视频路径关联 mapper
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@UseDataSource("embyboss")
@Mapper
public interface VideoPathRelationMapper extends BaseMapper<VideoPathRelation> {
}