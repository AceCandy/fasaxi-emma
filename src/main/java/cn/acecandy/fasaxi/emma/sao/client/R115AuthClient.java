package cn.acecandy.fasaxi.emma.sao.client;

import cn.acecandy.fasaxi.emma.sao.config.LogFeignConfig;
import cn.acecandy.fasaxi.emma.sao.out.R115RefreshTokenResp;
import cn.acecandy.fasaxi.emma.sao.out.RAuth115;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 115 auth client
 *
 * @author tangningzhu
 * @since 2025/8/15
 */
@FeignClient(name = "R115Client", url = "${cloud.r115.auth-host}",
        configuration = LogFeignConfig.class)
public interface R115AuthClient {

    /**
     * 获取访问令牌
     *
     * @param refreshToken 刷新令牌
     * @return {@link RAuth115 }<{@link R115RefreshTokenResp }>
     */
    @PostMapping(path = "/open/refreshToken", consumes = "application/x-www-form-urlencoded")
    RAuth115<R115RefreshTokenResp> refreshToken(@RequestParam("refresh_token") String refreshToken);

}