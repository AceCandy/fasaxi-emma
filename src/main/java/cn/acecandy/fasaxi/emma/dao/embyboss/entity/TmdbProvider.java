package cn.acecandy.fasaxi.emma.dao.embyboss.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Date;

/**
 * tmdb外部信息实体
 *
 * @author AceCandy
 * @since 2025/5/3
 */
@Table(value = "tmdb_provider", dataSource = "embyboss")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Data(staticConstructor = "x")
public class TmdbProvider extends Model<TmdbProvider> {

    /**
     * id
     */
    @Id(keyType = KeyType.Auto)
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