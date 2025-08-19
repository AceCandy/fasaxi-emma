package cn.acecandy.fasaxi.emma.sao.proxy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static cn.acecandy.fasaxi.emma.utils.CacheUtil.R_123_ZONG_TOKEN;

/**
 * r123-zong代理
 *
 * @author tangningzhu
 * @since 2025/8/18
 */
@Component
@Slf4j
public class R123ZongProxy extends R123Proxy {

    @Value("${cloud.r123.zong.client-id}")
    private String clientId;

    @Value("${cloud.r123.zong.client-secret}")
    private String clientSecret;

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public String getClientSecret() {
        return clientSecret;
    }

    @Override
    public String getCacheTokenKey() {
        return R_123_ZONG_TOKEN;
    }
}