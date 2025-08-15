package cn.acecandy.fasaxi.emma.sao.out;

import lombok.Data;

/**
 * @author tangningzhu
 * @since 2025/8/15
 */
@Data
public class R123TokenResp {
    /**
     * 访问凭证
     */
    private String accessToken;
    /**
     * access_token过期时间
     * <p>
     * 格式：2025-03-23T15:48:37+08:00
     */
    private String expiredAt;
}