package cn.acecandy.fasaxi.emma.sao.out;

import lombok.Data;

/**
 * r115 auth resp
 *
 * @author tangningzhu
 * @since 2025/8/19
 */
@Data
public class RAuth115<T> {
    private Integer state;
    private Integer code;
    private String message;
    private T data;
    private String error;
    private Integer errno;
}