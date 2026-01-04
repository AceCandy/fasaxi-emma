package cn.acecandy.fasaxi.emma.dao.toolkit.entity;

import cn.acecandy.fasaxi.emma.dao.toolkit.handler.JsonbTypeHandler;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;

/**
 * 媒体元数据
 *
 * @author AceCandy
 * @since 2025/10/21
 */
@Table(value = "media_metadata", dataSource = "toolkit")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Data(staticConstructor = "x")
public class MediaMetadata extends Model<MediaMetadata> {
    /**
     *
     */
    private String tmdbId;

    /**
     *
     */
    private String itemType;

    /**
     *
     */
    private String title;

    /**
     *
     */
    private String originalTitle;

    /**
     *
     */
    private Integer releaseYear;

    /**
     *
     */
    private Float rating;

    /**
     *
     */
    private String genresJson;

    /**
     *
     */
    private String actorsJson;

    /**
     *
     */
    private String directorsJson;

    /**
     *
     */
    private String studiosJson;

    /**
     *
     */
    private String countriesJson;

    /**
     *
     */
    private Date lastUpdatedAt;

    /**
     *
     */
    private Date releaseDate;

    /**
     *
     */
    private Date dateAdded;

    /**
     *
     */
    private Date lastSyncedAt;

    /**
     *
     */
    // private String officialRating;

    /**
     *
     */
    // private String unifiedRating;

    /**
     *
     */
    private Boolean inLibrary;


    /**
     *
     */
    @Column(typeHandler = JsonbTypeHandler.class)
    private List<String> embyItemIdsJson;

}