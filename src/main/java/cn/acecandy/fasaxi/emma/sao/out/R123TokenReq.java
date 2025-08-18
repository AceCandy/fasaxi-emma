package cn.acecandy.fasaxi.emma.sao.out;

import lombok.Builder;
import lombok.Data;

/**
 * 获取123 accessToken
 *
 * @author tangningzhu
 * @since 2025/8/15
 */
@Builder
@Data
public class R123TokenReq {
    /**
     * 客户端ID
     */
    private String clientID;
    /**
     * 客户端密钥
     */
    private String clientSecret;
}