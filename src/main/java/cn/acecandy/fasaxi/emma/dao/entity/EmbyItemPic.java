package cn.acecandy.fasaxi.emma.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @TableId
    @Schema(name = "项目id")
    private Integer itemId;

    @Schema(name = "封面图")
    private String posterPath;

    @Schema(name = "背景图")
    private String backdropPath;

    @Schema(name = "logo图")
    private String logoPath;

    @Schema(name = "创建时间")
    private Date createTime;

    @Schema(name = "更新时间")
    private Date updateTime;
}