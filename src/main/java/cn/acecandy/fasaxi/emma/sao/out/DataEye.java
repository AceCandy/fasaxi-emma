package cn.acecandy.fasaxi.emma.sao.out;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * dataeye 数据封装
 *
 * @author tangningzhu
 * @since 2026/1/21
 */
@Data
public class DataEye<T> {
    /**
     * 消息ID?
     */
    private Long id;
    /**
     * 状态码 200-成功
     */
    private Integer statusCode;

    @JsonIgnore
    public boolean isOk() {
        return statusCode == 200 || statusCode == 0;
    }

    private Page page;

    private T content;

    @Data
    public static class Page {
        /**
         * 当前页数
         */
        private Integer pageId;
        /**
         * 页面大小
         */
        private Integer pageSize;
        /**
         * 总记录数
         */
        private Long totalRecords;

    }
}