package cn.acecandy.fasaxi.emma.dao.toolkit.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;

/**
 * 自定义虚拟库
 *
 * @author AceCandy
 * @since 2025/10/20
 */
@Table(value = "custom_collections", dataSource = "toolkit")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Data(staticConstructor = "x")
public class CustomCollections extends Model<CustomCollections> {
    /**
     * 自增id
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 合集名称
     */
    private String name;

    /**
     * 类型 合集-list/筛选-filter
     */
    private String type;

    /**
     * 合集定义 json
     */
    private String definitionJson;

    /**
     * 状态 启用-active/暂停-paused
     */
    private String status;

    /**
     * emby合集id
     */
    private String embyCollectionId;

    /**
     * 最后同步时间
     */
    private Date lastSyncedAt;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 包含的媒体类型 Movie-电影 Series-电视剧
     * <p>
     * 混合为两种都包含["Movie", "Series"]
     */
    private String itemType;

    /**
     * 库中数量
     */
    private Integer inLibraryCount;

    /**
     * 构建的媒体信息 list
     */
    // @Column(typeHandler = JsonbTypeHandler.class)
    private List<String> generatedMediaInfoJson;

    /**
     * 封面图
     * <p>
     * 类似/Items/3027958/Images/Primary?tag=28a45bca3015a10d168b1409ac0be69b
     */
    // private String posterPath;

    /**
     * 在合集列表中的排序
     */
    private Integer sortOrder;

    /**
     * 允许的用户id list
     */
    private String allowedUserIds;

}