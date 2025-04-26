package cn.acecandy.fasaxi.emma.common.ex;


import cn.acecandy.fasaxi.emma.common.enums.ErrCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 业务异常封装
 *
 * @author AceCandy
 * @since 2024/09/14
 */
@Getter
public class BaseException extends RuntimeException {

    @Schema(title = "错误码")
    protected Integer code;

    @Schema(title = "错误信息")
    protected String msg;

    public BaseException(String msg) {
        super(msg);
        this.code = ErrCode.FAIL.getCode();
        this.msg = msg;
    }

    public BaseException(ErrCode errCode) {
        super(errCode.getMsg());
        this.code = errCode.getCode();
        this.msg = errCode.getMsg();
    }

    public BaseException(ErrCode errCode, String customMsg) {
        super(errCode.getMsg() + customMsg);
        this.code = errCode.getCode();
        this.msg = errCode.getMsg() + customMsg;
    }
}