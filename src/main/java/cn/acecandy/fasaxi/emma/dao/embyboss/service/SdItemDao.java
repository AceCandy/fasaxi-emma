package cn.acecandy.fasaxi.emma.dao.embyboss.service;

import cn.acecandy.fasaxi.emma.dao.embyboss.entity.SdItem;
import cn.acecandy.fasaxi.emma.dao.embyboss.mapper.SdItemMapper;
import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static cn.acecandy.fasaxi.emma.dao.embyboss.entity.table.SdItemTableDef.SD_ITEM;
import static com.mybatisflex.core.query.QueryMethods.max;

/**
 * 短剧基础维度表 dao
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@Slf4j
@UseDataSource("embyboss")
@Component
public class SdItemDao extends ServiceImpl<SdItemMapper, SdItem> {

    /**
     * 获取最大源id
     *
     * @return {@link Long }
     */
    public Long getMaxSourceId(Integer source) {
        return mapper.selectOneByQueryAs(query().select(max(SD_ITEM.SOURCE_ID)
                .as("maxSourceId")).where(SD_ITEM.SOURCE.eq(source)), Long.class);
    }
}