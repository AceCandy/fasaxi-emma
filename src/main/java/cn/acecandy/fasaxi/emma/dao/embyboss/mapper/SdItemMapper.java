package cn.acecandy.fasaxi.emma.dao.embyboss.mapper;

import cn.acecandy.fasaxi.emma.dao.embyboss.entity.SdItem;
import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 短剧基础维度表 mapper
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@UseDataSource("embyboss")
@Mapper
public interface SdItemMapper extends BaseMapper<SdItem> {
}