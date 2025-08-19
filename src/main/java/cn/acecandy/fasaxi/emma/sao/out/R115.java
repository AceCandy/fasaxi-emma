package cn.acecandy.fasaxi.emma.sao.out;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 123返回类封装
 *
 * @author tangningzhu
 * @since 2025/8/15
 */
@AllArgsConstructor
@Data
public class R115<T> {
    /**
     * 状态；true：正常；false：异常
     */
    private boolean state;
    /**
     * 异常码
     */
    private Integer code;
    /**
     * 异常信息
     */
    private String message;
    /**
     * 数据
     */
    private T data;

    @JsonIgnore
    public boolean isOk() {
        return state;
    }
}