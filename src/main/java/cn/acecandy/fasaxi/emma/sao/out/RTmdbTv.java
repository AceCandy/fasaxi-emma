package cn.acecandy.fasaxi.emma.sao.out;

import cn.hutool.v7.core.annotation.Alias;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * tmdb tv返回
 *
 * @author tangningzhu
 * @since 2025/10/29
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class RTmdbTv extends RTmdbMedia {


    /**
     * 原产国列表
     */
    @Alias("origin_country")
    private List<String> originCountry;

    /**
     * 原始名称
     */
    @Alias("original_name")
    private String originalName;

    /**
     * 首次播出日期
     */
    @Alias("first_air_date")
    private String firstAirDate;

    /**
     * 节目名称
     */
    @Alias("name")
    private String name;
}