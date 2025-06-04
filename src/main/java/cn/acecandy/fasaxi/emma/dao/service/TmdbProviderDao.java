package cn.acecandy.fasaxi.emma.dao.service;

import cn.acecandy.fasaxi.emma.dao.entity.TmdbProvider;
import cn.acecandy.fasaxi.emma.dao.mapper.TmdbProviderMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
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

    public boolean insertOrUpdate(TmdbProvider dto) {
        if (dto == null) {
            return false;
        }
        return baseMapper.insertOrUpdate(dto);
    }
}