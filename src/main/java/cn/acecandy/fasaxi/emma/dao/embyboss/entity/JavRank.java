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
 * jav榜单实体类
 *
 * @author AceCandy
 * @since 2026-01-18
 */
@Table(value = "jav_rank", dataSource = "embyboss")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Data(staticConstructor = "x")
public class JavRank extends Model<JavRank> {

    /**
     * 内部自增ID
     */
    @Id
    private Long id;

    /**
     * 排名类型 影片榜/JavDB 有码 TOP250/JavLibray TOP500
     */
    private String rankType;

    /**
     * 排名时间
     */
    private String rankDate;

    /**
     * 排名列表 list结构
     */
    @Column(typeHandler = JacksonTypeHandler.class)
    private List<String> serList;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}