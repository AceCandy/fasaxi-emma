package cn.acecandy.fasaxi.emma.dao.ms.service;

import cn.acecandy.fasaxi.emma.dao.ms.entity.MediaServerSyncItems;
import cn.acecandy.fasaxi.emma.dao.ms.mapper.MediaServerSyncItemsMapper;
import cn.acecandy.fasaxi.emma.dao.toolkit.entity.CustomCollections;
import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

import static cn.acecandy.fasaxi.emma.dao.ms.entity.table.MediaServerSyncItemsTableDef.MEDIA_SERVER_SYNC_ITEMS;


/**
 * 媒体元数据 dao
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@Slf4j
@UseDataSource("ms")
@Component
public class MediaServerSyncItemsDao extends ServiceImpl<MediaServerSyncItemsMapper, MediaServerSyncItems> {

    /**
     * 按tmdbIds查找
     *
     * @param tmdbIds tmdbName
     * @return {@link List }<{@link CustomCollections }>
     */
    public List<MediaServerSyncItems> findByTmdbId(List<Integer> tmdbIds, String itemType) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(MEDIA_SERVER_SYNC_ITEMS.TMDB_ID.in(tmdbIds))
                .and(MEDIA_SERVER_SYNC_ITEMS.ITEM_TYPE.eq(itemType));
        return mapper.selectListByQuery(wrapper);
    }
}