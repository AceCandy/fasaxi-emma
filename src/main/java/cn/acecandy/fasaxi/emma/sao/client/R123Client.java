package cn.acecandy.fasaxi.emma.sao.client;

import cn.acecandy.fasaxi.emma.sao.out.R123;
import cn.acecandy.fasaxi.emma.sao.out.R123TokenResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 123 client
 *
 * @author tangningzhu
 * @since 2025/8/15
 */
@Component
@Slf4j
public class R123Client {

    R123<R123TokenResp> getAccessToken(@RequestBody AccessTokenReq req){
        String uri = "https://open-api.123pan.com/api/v1/access_token";
    }
}