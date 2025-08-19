package cn.acecandy.fasaxi.emma.sao.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 115刷新token
 *
 * @author tangningzhu
 * @since 2025/8/15
 */
@Data
public class R115RefreshTokenResp {
    /**
     * 访问令牌
     * <p>
     * 同时刷新有效期
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * 刷新令牌
     * <p>
     * 有效期不延长不改变
     */
    @JsonProperty("refresh_token")
    private String refreshToken;

    /**
     * 到期时间剩余
     * <p>
     * 有效期，单位秒
     */
    @JsonProperty("expires_in")
    private long expiresIn;
}