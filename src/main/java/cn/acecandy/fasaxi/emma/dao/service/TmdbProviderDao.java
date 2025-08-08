package cn.acecandy.fasaxi.emma.dao.service;

import cn.acecandy.fasaxi.emma.dao.entity.TmdbProvider;
import cn.acecandy.fasaxi.emma.dao.mapper.TmdbProviderMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.text.StrUtil;
import org.springframework.stereotype.Component;

import java.util.List;

import static cn.acecandy.fasaxi.emma.dao.entity.table.TmdbProviderTableDef.TMDB_PROVIDER;

/**
 * tmdb外部信息 dao
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@Slf4j
@Component
public class TmdbProviderDao extends ServiceImpl<TmdbProviderMapper, TmdbProvider> {

    /**
     * 插入或更新
     *
     * @param dto 到
     * @return boolean
     */
    public boolean insertOrUpdate(TmdbProvider dto) {
        if (dto == null) {
            return false;
        }
        return mapper.insertOrUpdateSelective(dto) > 0;
    }

    /**
     * 通过tmdb查询
     *
     * @param tmdbId   tmdb id
     * @param embyType Emby类型
     * @return boolean
     */
    public TmdbProvider findByTmdb(String tmdbId, String embyType) {
        if (StrUtil.isBlank(tmdbId) || StrUtil.isBlank(embyType)) {
            return null;
        }

        QueryWrapper wrapper = QueryWrapper.create()
                .where(TMDB_PROVIDER.TMDB_ID.eq(tmdbId))
                .and(TMDB_PROVIDER.EMBY_TYPE.eq(embyType));
        return mapper.selectOneByQuery(wrapper);
    }

    public List<TmdbProvider> findAllImdbNoDouBan() {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(TMDB_PROVIDER.IMDB_ID.isNotNull())
                .and(TMDB_PROVIDER.DOUBAN_ID.isNull());
        return mapper.selectListByQuery(wrapper);
    }

    public List<TmdbProvider> findAllNoDouBanInfo() {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(TMDB_PROVIDER.DOUBAN_ID.isNotNull())
                .and(TMDB_PROVIDER.DOUBAN_INFO.isNull());
        return mapper.selectListByQuery(wrapper);
    }
}