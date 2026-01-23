package cn.acecandy.fasaxi.emma.dao.embyboss.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.activerecord.Model;
import com.mybatisflex.core.handler.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 剧查查-短剧榜单表实体类
 *
 * @author AceCandy
 * @since 2026-01-23
 */
@Table(value = "sd_jcc_rank", dataSource = "embyboss")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Data(staticConstructor = "x")
public class SdJccRank extends Model<SdJccRank> {

    /**
     * 内部自增ID
     */
    @Id
    private Long id;

    /**
     * 榜单类型 如:1-热力榜
     */
    private Integer type;

    /**
     * 时间阶段 day-日 month-月
     */
    private String timeStage;

    /**
     * 对应时间
     */
    private String timeValue;

    /**
     * 短剧id列表
     * 数据库类型为 JSON，映射为 Java 的 List<Long>
     */
    @Column(typeHandler = JacksonTypeHandler.class)
    private List<Long> rankList;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}