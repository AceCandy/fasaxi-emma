package cn.acecandy.fasaxi.emma.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * tmdb外部信息实体
 *
 * @author AceCandy
 * @since 2025/5/3
 */
@Builder
@Data
public class TmdbProvider {

    @TableId
    @Schema(name = "自增id")
    private Long id;

    @Schema(name = "类型 Movie-电影 Series-电视剧 参考EmbyMediaType")
    private String embyType;

    @Schema(name = "tmdbId")
    private String tmdbId;

    @Schema(name = "imdbId")
    private String imdbId;

    @Schema(name = "tvdbId")
    private String tvdbId;

    @Schema(name = "doubanId")
    private String doubanId;

    @Schema(name = "tmdb json")
    private String tmdbInfo;

    @Schema(name = "douban json")
    private String doubanInfo;

    @Schema(name = "douban评分")
    private BigDecimal doubanRate;

    @Schema(name = "创建时间")
    private Date createTime;

    @Schema(name = "更新时间")
    private Date updateTime;

    @Schema(name = "评分更新时间")
    private Date rateUpdateTime;
}