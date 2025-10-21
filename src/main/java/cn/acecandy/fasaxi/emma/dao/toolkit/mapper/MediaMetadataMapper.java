package cn.acecandy.fasaxi.emma.dao.toolkit.mapper;

import cn.acecandy.fasaxi.emma.dao.toolkit.entity.MediaMetadata;
import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 媒体元数据 mapper
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@UseDataSource("toolkit")
@Mapper
public interface MediaMetadataMapper extends BaseMapper<MediaMetadata> {
}