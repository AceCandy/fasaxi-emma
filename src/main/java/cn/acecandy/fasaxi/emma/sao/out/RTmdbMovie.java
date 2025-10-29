package cn.acecandy.fasaxi.emma.sao.out;

import cn.hutool.v7.core.annotation.Alias;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * tmdb movie返回
 *
 * @author tangningzhu
 * @since 2025/10/29
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class RTmdbMovie extends RTmdbMedia {


    /**
     * 原始标题
     */
    @Alias("original_title")
    private String originalTitle;


    /**
     * 发布日期
     */
    @Alias("release_date")
    private String releaseDate;

    /**
     * 标题
     */
    @Alias("title")
    private String title;

    /**
     * 是否有视频
     */
    @Alias("video")
    private Boolean video;
}