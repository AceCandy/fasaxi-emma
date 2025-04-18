package cn.acecandy.fasaxi.emma.common.req;

import lombok.Data;

/**
 * Emby授权 请求入参
 *
 * @author AceCandy
 * @since 2025/04/15
 */
@Data
public class EmbyAuthReq {
    /**
     * 用户名
     */
    private String Username;
    /**
     * 密码
     */
    private String Pw;
}