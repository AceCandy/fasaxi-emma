package cn.acecandy.fasaxi.emma.dao.embyboss.service;

import cn.acecandy.fasaxi.emma.dao.embyboss.entity.SdJccRank;
import cn.acecandy.fasaxi.emma.dao.embyboss.mapper.SdJccRankMapper;
import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 剧查查-短剧榜单表 dao
 *
 * @author AceCandy
 * @since 2026-01-23
 */
@Slf4j
@UseDataSource("embyboss")
@Component
public class SdJccRankDao extends ServiceImpl<SdJccRankMapper, SdJccRank> {
}
