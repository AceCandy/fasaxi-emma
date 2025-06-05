package cn.acecandy.fasaxi.emma.dao.service;

import cn.acecandy.fasaxi.emma.dao.entity.TmdbProvider;
import cn.acecandy.fasaxi.emma.dao.mapper.TmdbProviderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.text.StrUtil;
import org.springframework.stereotype.Component;

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
        return baseMapper.insertOrUpdate(dto);
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
        LambdaQueryWrapper<TmdbProvider> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmdbProvider::getTmdbId, tmdbId)
                .eq(TmdbProvider::getEmbyType, embyType);
        ;
        return baseMapper.selectOne(wrapper);
    }
}