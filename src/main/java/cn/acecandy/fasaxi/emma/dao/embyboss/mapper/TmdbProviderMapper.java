package cn.acecandy.fasaxi.emma.dao.embyboss.mapper;

import cn.acecandy.fasaxi.emma.dao.embyboss.entity.TmdbProvider;
import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * tmdb信息 mapper
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@UseDataSource("embyboss")
@Mapper
public interface TmdbProviderMapper extends BaseMapper<TmdbProvider> {
}