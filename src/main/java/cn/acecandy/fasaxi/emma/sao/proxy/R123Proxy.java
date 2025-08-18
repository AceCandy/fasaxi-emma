package cn.acecandy.fasaxi.emma.sao.proxy;

import cn.acecandy.fasaxi.emma.common.ex.BaseException;
import cn.acecandy.fasaxi.emma.sao.client.R123Client;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.out.R123;
import cn.acecandy.fasaxi.emma.sao.out.R123TokenReq;
import cn.acecandy.fasaxi.emma.sao.out.R123TokenResp;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.text.StrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static cn.acecandy.fasaxi.emma.common.enums.ErrCode.ERRCODE_1001;
import static cn.acecandy.fasaxi.emma.common.enums.ErrCode.ERRCODE_1002;

/**
 * r123代理
 *
 * @author tangningzhu
 * @since 2025/8/18
 */
@Component
@Slf4j
public class R123Proxy {

    @Resource
    private R123Client r123Client;

    @Value("${cloud.r123.client-id}")
    private String clientId;

    @Value("${cloud.r123.client-secret}")
    private String clientSecret;

    @Resource
    private RedisClient redisClient;

    /**
     * 获取访问令牌
     *
     * @return {@link R123TokenResp }
     */
    public R123TokenResp getAccessToken() {
        if (StrUtil.isBlank(clientSecret) || StrUtil.isBlank(clientId)) {
            throw new BaseException(ERRCODE_1001);
        }
        R123<R123TokenResp> result = r123Client
                .getAccessToken(R123TokenReq.builder().clientID(clientId).clientSecret(clientSecret).build());
        if (result == null || !result.isOk()) {
            throw new BaseException(ERRCODE_1002,
                    StrUtil.format(" getAccessToken,resp:{}", result));
        }
        return result.getData();
    }

}