package cn.acecandy.fasaxi.emma.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码
 *
 * @author AceCandy
 * @since 2024/09/14
 */
@AllArgsConstructor
@Getter
public enum ErrCode {
    // 默认成功码
    DEFAULT_CODE(0, "ok"),
    // 通用错误码,
    FAIL(-1, "fail"),
    ERRCODE_400(400, "参数校验不通过!"),
    ERRCODE_404(404, "请求的资源未找到!"),
    ERRCODE_405(405, "请求方式非法!"),
    ERRCODE_500(500, "服务器错误,未捕获异常,请联系开发人员排查!"),

    ;

    private final Integer code;
    private final String msg;
}