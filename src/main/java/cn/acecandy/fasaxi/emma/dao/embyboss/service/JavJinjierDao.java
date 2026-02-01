package cn.acecandy.fasaxi.emma.dao.embyboss.service;

import cn.acecandy.fasaxi.emma.dao.embyboss.entity.JavJinjier;
import cn.acecandy.fasaxi.emma.dao.embyboss.mapper.JavJinjierMapper;
import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * jav-金鸡儿奖表 dao
 *
 * @author AceCandy
 * @since 2026-01-18
 */
@Slf4j
@UseDataSource("embyboss")
@Component
public class JavJinjierDao extends ServiceImpl<JavJinjierMapper, JavJinjier> {

}