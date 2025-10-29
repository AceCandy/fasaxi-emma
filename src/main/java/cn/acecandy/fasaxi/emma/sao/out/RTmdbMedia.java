package cn.acecandy.fasaxi.emma.sao.out;

import cn.hutool.v7.core.annotation.Alias;
import lombok.Data;

import java.util.List;

/**
 * tmdb movie返回
 *
 * @author tangningzhu
 * @since 2025/10/29
 */
@Data
public class RTmdbMedia {

    /**
     * 是否成人内容
     */
    @Alias("adult")
    private Boolean adult;

    /**
     * 背景图片路径
     */
    @Alias("backdrop_path")
    private String backdropPath;

    /**
     * 类型ID列表
     */
    @Alias("genre_ids")
    private List<Integer> genreIds;

    /**
     * 电影ID
     */
    @Alias("id")
    private Integer id;

    /**
     * 原始语言
     */
    @Alias("original_language")
    private String originalLanguage;

    /**
     * 电影概述
     */
    @Alias("overview")
    private String overview;

    /**
     * 流行度
     */
    @Alias("popularity")
    private Double popularity;

    /**
     * 海报路径
     */
    @Alias("poster_path")
    private String posterPath;

    /**
     * 评分平均值
     */
    @Alias("vote_average")
    private Double voteAverage;

    /**
     * 评分数量
     */
    @Alias("vote_count")
    private Integer voteCount;
}