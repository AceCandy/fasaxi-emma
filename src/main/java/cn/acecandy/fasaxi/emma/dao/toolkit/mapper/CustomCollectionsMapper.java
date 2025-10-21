package cn.acecandy.fasaxi.emma.dao.toolkit.mapper;

import cn.acecandy.fasaxi.emma.dao.toolkit.entity.CustomCollections;
import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自定义合集 mapper
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@UseDataSource("toolkit")
@Mapper
public interface CustomCollectionsMapper extends BaseMapper<CustomCollections> {
}