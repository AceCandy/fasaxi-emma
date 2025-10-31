package cn.acecandy.fasaxi.emma.dao.embyboss.service;

import cn.acecandy.fasaxi.emma.dao.embyboss.entity.TmdbProvider;
import cn.acecandy.fasaxi.emma.dao.embyboss.mapper.TmdbProviderMapper;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.text.StrUtil;
import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

import static cn.acecandy.fasaxi.emma.dao.embyboss.entity.table.TmdbProviderTableDef.TMDB_PROVIDER;

/**
 * tmdb外部信息 dao
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@Slf4j
@UseDataSource("embyboss")
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

    /**
     * 通过doubanId查询
     *
     * @param doubanId 豆瓣 id
     * @param embyType Emby类型
     * @return boolean
     */
    public List<TmdbProvider> findByDouban(List<String> doubanId, String embyType) {
        if (CollUtil.isEmpty(doubanId) || StrUtil.isBlank(embyType)) {
            return null;
        }

        QueryWrapper wrapper = QueryWrapper.create()
                .where(TMDB_PROVIDER.DOUBAN_ID.in(doubanId))
                .and(TMDB_PROVIDER.EMBY_TYPE.eq(embyType));
        return mapper.selectListByQuery(wrapper);
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