package cn.acecandy.fasaxi.emma.sao.out;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 123返回类封装
 *
 * @author tangningzhu
 * @since 2025/8/15
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
public class R115Search<T> extends R115<T> {
    /**
     * 搜索符合条件的文件(夹)总数
     */
    private Integer count;
    /**
     * 分页获取值
     */
    private Integer limit;
    /**
     * 偏移
     */
    private Integer offset;
}