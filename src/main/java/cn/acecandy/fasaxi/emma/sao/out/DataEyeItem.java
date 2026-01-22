package cn.acecandy.fasaxi.emma.sao.out;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 短剧基础维度表实体类
 * 对应表名：short_drama_items
 *
 * @author AceCandy
 * @since 2026/01/21
 */
@Data
public class DataEyeItem {

    /**
     * 来源方ID
     */
    private Long playletId;

    /**
     * 短剧名称
     */
    private String playletName;

    /**
     * 标签列表
     */
    private List<String> playletTagNames;

    /**
     * 封面图链接
     */
    private String coverOss;

    /**
     * 简介
     */
    private String description;

    /**
     * 总集数
     */
    private Integer totalEpisode;

    /**
     * 首发日期
     */
    private LocalDate releaseStartDate;

    /**
     * 完结/最后更新日期
     */
    private LocalDate releaseEndDate;
}