package cn.acecandy.fasaxi.emma.dao.embyboss.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 视频路径关联表
 *
 * @author AceCandy
 * @since 2025/5/3
 */
@Table(value = "video_path_relation", dataSource = "embyboss")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Data(staticConstructor = "x")
public class VideoPathRelation extends Model<VideoPathRelation> {

    /**
     * 媒体id
     */
    @Id
    private Integer itemId;

    /**
     * 媒体名：电影是Name，电视剧是SeriesName+SeasonName+Name
     */
    private String itemName;


    /**
     * 媒体类型：Movie-电影/Episode-电视剧
     */
    private String itemType;

    /**
     * 备份状态 0-未备份 1-备份中 2-备份完成 默认0
     */
    private Integer bakStatus;

    /**
     * 对应数据中的Path字段
     */
    private String strmPath;

    /**
     * 路径类型：local/115/123/micu（默认local）
     */
    private String strmType;

    /**
     * 解析媒体信息中的真实路径
     */
    private String realPath;
    /**
     * 路径前缀
     */
    private String pathPrefix;
    /**
     * 剔除域名和前缀的路径
     */
    private String purePath;

    /**
     * emby入库的更新时间
     */
    private Date embyTime;

    /**
     * strm创建的更新时间
     */
    private Date strmTime;

    /**
     * 对应115路径
     */
    @Column("115_path")
    private String path115;

    /**
     * 对应123路径
     */
    @Column("123_path")
    private String path123;

    /**
     * 创建时间（默认当前时间戳）
     */
    private Date createTime;

    /**
     * 更新时间（默认当前时间戳，更新时自动刷新）
     */
    private Date updateTime;
}