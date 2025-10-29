package cn.acecandy.fasaxi.emma.sao.out;

import cn.hutool.v7.core.annotation.Alias;
import lombok.Data;

import java.util.List;

/**
 * tmdb通用返回
 *
 * @author tangningzhu
 * @since 2025/10/29
 */
@Data
public class RTmdb<T> {

    /**
     * 当前页码
     */
    @Alias("page")
    private Integer page;

    /**
     * 列表 区分电影电视剧
     */
    @Alias("results")
    private List<T> results;

    /**
     * 总页数
     */
    @Alias("total_pages")
    private Integer totalPages;

    /**
     * 总结果数
     */
    @Alias("total_results")
    private Integer totalResults;
}