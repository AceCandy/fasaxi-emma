package cn.acecandy.fasaxi.emma.dao.embyboss.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.activerecord.Model;
import com.mybatisflex.core.handler.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * jav-金鸡儿奖表实体类
 *
 * @author AceCandy
 * @since 2026-01-18
 */
@Table(value = "jav_jinjier", dataSource = "embyboss")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Data(staticConstructor = "x")
public class JavJinjier extends Model<JavJinjier> {

    /**
     * 内部自增ID
     */
    @Id
    private Long id;

    /**
     * 番号
     */
    private String serNum;

    /**
     * 发行日期
     */
    private LocalDate releaseDate;

    /**
     * 第几届
     */
    private Integer season;

    /**
     * 奖项标签 list结构
     */
    @Column(typeHandler = JacksonTypeHandler.class)
    private List<String> prizeTag;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}