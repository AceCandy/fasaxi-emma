package cn.acecandy.fasaxi.emma.dao.ms.entity;

import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 媒体元数据
 *
 * @author AceCandy
 * @since 2025/10/21
 */
@Table(value = "media_server_sync_items", dataSource = "ms")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Data(staticConstructor = "x")
public class MediaServerSyncItems extends Model<MediaServerSyncItems> {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 媒体服务器ID
     */
    private Long mediaServerId;

    /**
     * 库标识
     */
    private String library;

    /**
     * 库名称
     */
    private String libraryName;

    /**
     * 项目ID
     */
    private String itemId;

    /**
     * 项目类型
     */
    private String itemType;

    /**
     * 标题
     */
    private String title;

    /**
     * 原始标题
     */
    private String originTitle;

    /**
     * 年份
     */
    private Long year;

    /**
     * TMDB ID
     */
    private Integer tmdbId;

    /**
     * IMDB ID
     */
    private String imdbId;

    /**
     * 文件路径
     */
    private String path;

    /**
     * 文件大小
     */
    private Long size;

    /**
     * 剧集信息（JSON格式）
     */
    // @Column(typeHandler = JsonbTypeHandler.class)
    // private List<> episodes;

    /**
     * 是否缺少剧集
     */
    private Boolean isMissEps;

    /**
     * 创建时间（时间戳）
     */
    private Long createdAt;

    /**
     * 更新时间（时间戳）
     */
    private Long updatedAt;
}