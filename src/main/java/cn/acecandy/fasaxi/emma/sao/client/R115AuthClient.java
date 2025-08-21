package cn.acecandy.fasaxi.emma.sao.client;

import cn.acecandy.fasaxi.emma.sao.config.LogFeignConfig;
import cn.acecandy.fasaxi.emma.sao.out.R115Auth;
import cn.acecandy.fasaxi.emma.sao.out.R115RefreshTokenReq;
import cn.acecandy.fasaxi.emma.sao.out.R115RefreshTokenResp;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 115 auth client
 *
 * @author tangningzhu
 * @since 2025/8/15
 */
@FeignClient(name = "R115AuthClient", url = "${cloud.r115.auth-host}",
        configuration = LogFeignConfig.class)
public interface R115AuthClient {

    /**
     * 获取访问令牌
     *
     * @param req 刷新令牌
     * @return {@link R115Auth }<{@link R115RefreshTokenResp }>
     */
    @PostMapping(path = "/open/refreshToken", consumes = "application/x-www-form-urlencoded")
    R115Auth<R115RefreshTokenResp> refreshToken(R115RefreshTokenReq req);

}