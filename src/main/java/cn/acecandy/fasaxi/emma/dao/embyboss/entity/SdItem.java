package cn.acecandy.fasaxi.emma.dao.embyboss.entity;

import cn.hutool.v7.core.date.DateTime;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.activerecord.Model;
import com.mybatisflex.core.handler.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.util.List;

/**
 * 短剧基础维度表实体类
 * 对应表名：short_drama_items
 *
 * @author AceCandy
 * @since 2026/01/21
 */
@Table(value = "sd_item", dataSource = "embyboss")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Data(staticConstructor = "x")
public class SdItem extends Model<SdItem> {

    /**
     * 内部自增ID
     */
    @Id
    private Long id;

    /**
     * 来源方 如:1-剧查查
     * 建议在代码中定义一个 Enum 来维护这个值
     */
    private Integer source;

    /**
     * 来源方ID
     */
    private Long sourceId;

    /**
     * 短剧名称
     */
    private String title;

    /**
     * 标签列表
     * 数据库类型为 JSON，映射为 Java 的 List<String>
     */
    @Column(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    /**
     * 封面图链接
     */
    private String coverUrl;

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


    /**
     * 入库时间
     */
    private DateTime createdTime;

    /**
     * 更新时间
     */
    private DateTime updatedTime;
}