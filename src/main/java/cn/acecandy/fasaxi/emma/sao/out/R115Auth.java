package cn.acecandy.fasaxi.emma.sao.out;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import static cn.acecandy.fasaxi.emma.common.enums.ErrCode.DEFAULT_CODE;

/**
 * r115 auth resp
 *
 * @author tangningzhu
 * @since 2025/8/19
 */
@Data
public class R115Auth<T> {
    private Integer state;
    private Integer code;
    private String message;
    private T data;
    private String error;
    private Integer errno;

    @JsonIgnore
    public boolean isOk() {
        return DEFAULT_CODE.getCode().equals(code);
    }
}