package cn.acecandy.fasaxi.emma.sao.out;

import lombok.Builder;
import lombok.Data;

/**
 * 115刷新token
 *
 * @author tangningzhu
 * @since 2025/8/15
 */
@Builder
@Data
public class R115RefreshTokenReq {

    /**
     * 刷新令牌
     * <p>
     * 有效期不延长不改变
     */
    private String refresh_token;
}