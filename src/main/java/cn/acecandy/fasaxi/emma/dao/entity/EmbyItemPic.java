package cn.acecandy.fasaxi.emma.dao.entity;

import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * emby项目图片实体类
 *
 * @author AceCandy
 * @since 2025/5/3
 */
@Table("emby_item_pic")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Data(staticConstructor = "x")
public class EmbyItemPic extends Model<EmbyItemPic> {

    /**
     * 项目id
     */
    // @Id
    private Integer itemId;

    /**
     * 封面图
     */
    private String posterPath;

    /**
     * 背景图
     */
    private String backdropPath;

    /**
     * logo图
     */
    private String logoPath;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}