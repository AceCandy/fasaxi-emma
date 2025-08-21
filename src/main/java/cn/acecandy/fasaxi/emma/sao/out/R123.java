package cn.acecandy.fasaxi.emma.sao.out;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import static cn.acecandy.fasaxi.emma.common.enums.ErrCode.DEFAULT_CODE;

/**
 * 123返回类封装
 *
 * @author tangningzhu
 * @since 2025/8/15
 */
@Data
public class R123<T> {
    private Integer code;
    private String message;
    private T data;

    @JsonIgnore
    public boolean isOk() {
        return DEFAULT_CODE.getCode().equals(code);
    }
}