package cn.acecandy.fasaxi.emma.service;

import cn.acecandy.fasaxi.emma.config.EmbyConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * FastEmby服务
 *
 * @author AceCandy
 * @since 2024/11/19
 */
@Slf4j
@Component
public class FastEmbyService {

    @Resource
    private EmbyConfig embyConfig;


}