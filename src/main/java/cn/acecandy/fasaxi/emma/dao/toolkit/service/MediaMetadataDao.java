package cn.acecandy.fasaxi.emma.dao.toolkit.service;

import cn.acecandy.fasaxi.emma.dao.toolkit.entity.CustomCollections;
import cn.acecandy.fasaxi.emma.dao.toolkit.entity.MediaMetadata;
import cn.acecandy.fasaxi.emma.dao.toolkit.mapper.MediaMetadataMapper;
import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

import static cn.acecandy.fasaxi.emma.dao.toolkit.entity.table.MediaMetadataTableDef.MEDIA_METADATA;

/**
 * 媒体元数据 dao
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@Slf4j
@UseDataSource("toolkit")
@Component
public class MediaMetadataDao extends ServiceImpl<MediaMetadataMapper, MediaMetadata> {

    /**
     * 按embyId和排序字段查找
     *
     * @param embyIds   emby ids
     * @param sortBy    排序依据
     * @param sortOrder 排序顺序
     * @param showLimit 显示限制
     * @return {@link List }<{@link CustomCollections }>
     */
    public List<MediaMetadata> findByEmbyIdOrder(List<String> embyIds,
                                                 List<QueryColumn> sortBy,
                                                 boolean sortOrder, int showLimit) {
        String arrayLiteral = "{" + String.join(",", embyIds) + "}";
        QueryWrapper wrapper = QueryWrapper.create()
                .where("jsonb_exists_any(emby_item_ids_json,?::text[])", arrayLiteral);
        sortBy.forEach(sort -> wrapper.orderBy(sort, sortOrder));
        wrapper.limit(0, showLimit);
        return mapper.selectListByQuery(wrapper);
    }

    /**
     * 按embyIds查找
     *
     * @param embyIds emby ids
     * @return {@link List }<{@link CustomCollections }>
     */
    public List<MediaMetadata> findByEmbyId(List<String> embyIds, String itemType) {
        String arrayLiteral = "{" + String.join(",", embyIds) + "}";
        QueryWrapper wrapper = QueryWrapper.create()
                .where("jsonb_exists_any(emby_item_ids_json,?::text[])", arrayLiteral)
                .and(MEDIA_METADATA.ITEM_TYPE.eq(itemType));
        return mapper.selectListByQuery(wrapper);
    }

    /**
     * 按tmdbIds查找
     *
     * @param tmdbIds emby ids
     * @return {@link List }<{@link CustomCollections }>
     */
    public List<MediaMetadata> findByTmdbId(List<String> tmdbIds) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(MEDIA_METADATA.TMDB_ID.in(tmdbIds));
        return mapper.selectListByQuery(wrapper);
    }

}