package cn.acecandy.fasaxi.emma.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
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

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Emby类型 Movie-电影 Series-电视剧 参考EmbyMediaType
     */
    private String embyType;

    /**
     * tmdb id
     */
    private String tmdbId;

    /**
     * IMDb ID
     */
    private String imdbId;

    /**
     * TVDB ID
     */
    private String tvdbId;

    /**
     * Douban ID
     */
    private String doubanId;

    /**
     * tmdb 信息
     */
    private String tmdbInfo;

    /**
     * douban json
     */
    private String doubanInfo;

    /**
     * douban评分
     */
    private BigDecimal doubanRate;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 评分更新时间
     */
    private Date rateUpdateTime;
}