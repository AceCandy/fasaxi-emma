package cn.acecandy.fasaxi.emma.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * emby项目图片实体类
 *
 * @author AceCandy
 * @since 2025/5/3
 */
@Builder
@Data
public class EmbyItemPic {

    /**
     * 项目id
     */
    @TableId
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