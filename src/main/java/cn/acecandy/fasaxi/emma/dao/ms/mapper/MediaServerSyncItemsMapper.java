package cn.acecandy.fasaxi.emma.dao.ms.mapper;

import cn.acecandy.fasaxi.emma.dao.ms.entity.MediaServerSyncItems;
import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 媒体元数据 mapper
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@UseDataSource("ms")
@Mapper
public interface MediaServerSyncItemsMapper extends BaseMapper<MediaServerSyncItems> {
}