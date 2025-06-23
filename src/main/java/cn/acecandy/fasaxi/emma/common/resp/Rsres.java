package cn.acecandy.fasaxi.emma.common.resp;

import cn.acecandy.fasaxi.emma.common.enums.ErrCode;
import cn.acecandy.fasaxi.emma.common.ex.BaseException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;

import static cn.acecandy.fasaxi.emma.common.enums.ErrCode.DEFAULT_CODE;


/**
 * 通用返回
 *
 * @author AceCandy
 * @since 2024/09/14
 */
@AllArgsConstructor
@Data
public class Rsres<T> {

    /**
     * 响应编码 0-正常，小于0-系统级错误，大于0-业务级错误
     */
    private Integer returncode;

    /**
     * 响应消息 code非0时，message非空
     */
    private String msg;

    /**
     * 响应结果 接口的具体相应结果
     */
    private T result;

    private static <T> Rsres<T> of(int returncode, String msg, T result) {
        return new Rsres<>(returncode, msg, result);
    }

    private static <T> Rsres<T> of(ErrCode code, T result) {
        return of(code.getCode(), code.getMsg(), result);
    }

    public static <T> Rsres<T> success(T result) {
        return of(DEFAULT_CODE, result);
    }

    public static <T> Rsres<T> success() {
        return of(DEFAULT_CODE, null);
    }

    public static <T> Rsres<T> fail(BaseException ex) {
        return of(ex.getCode(), ex.getMsg(), null);
    }

    public static <T> Rsres<T> fail(ErrCode errCode, String msg) {
        return of(errCode.getCode(), errCode.getMsg() + msg, null);
    }

    public static <T> Rsres<T> fail(ErrCode errCode) {
        return of(errCode, null);
    }

    public static <T> Rsres<T> fail(String msg) {
        return of(ErrCode.FAIL.getCode(), msg, null);
    }

    public static <T> Rsres<T> fail() {
        return of(ErrCode.FAIL, null);
    }

    @JsonIgnore
    public boolean isOk() {
        return returncode == 0;
    }

}