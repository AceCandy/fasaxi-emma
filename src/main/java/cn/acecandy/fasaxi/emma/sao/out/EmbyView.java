package cn.acecandy.fasaxi.emma.sao.out;

import cn.hutool.v7.core.annotation.Alias;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * emby view信息输出类
 *
 * @author tangningzhu
 * @since 2025/4/16
 */
@Builder
@Data
public class EmbyView {
    /**
     * 服务器id
     */
    @Alias("ServerId")
    private String serverId;

    /**
     * 标题
     */
    @Alias("Name")
    private String name;
    /**
     * 项目id
     */
    @Alias("Id")
    private String id;

    @Alias("Guid")
    private String guid;

    @Alias("Etag")
    private String etag;

    @Alias("DateCreated")
    private String dateCreated;

    @Alias("DateModified")
    private String dateModified;

    @Alias("CanDelete")
    private Boolean canDelete;

    @Alias("CanDownload")
    private Boolean canDownload;

    /**
     * 唯一key tmdb会在上面标识
     */
    @Alias("PresentationUniqueKey")
    private String presentationUniqueKey;

    @Alias("SortName")
    private String sortName;

    @Alias("ForcedSortName")
    private String forcedSortName;

    @Alias("ExternalUrls")
    private List<String> externalUrls;

    @Alias("Taglines")
    private List<String> taglines;

    @Alias("RemoteTrailers")
    private List<String> remoteTrailers;

    @Alias("ProviderIds")
    private Map<String, String> providerIds;

    /**
     * 是否文件夹
     */
    @Alias("IsFolder")
    private Boolean isFolder;

    /**
     * 对应的父id
     * ps：剧集上一级是季 季上一级才是剧集
     */
    @Alias("ParentId")
    private String parentId;

    /**
     * 类型
     * 参考 EmbyMediaType
     */
    @Alias("Type")
    private String type;

    /**
     * 用户信息
     */
    @Alias("UserData")
    private Map<String, Object> userData;

    @Alias("ChildCount")
    private Integer childCount;

    @Alias("DisplayPreferencesId")
    private String displayPreferencesId;

    @Alias("PrimaryImageAspectRatio")
    private Double primaryImageAspectRatio;

    /**
     * 集合类型 movies、tvshows
     */
    @Alias("CollectionType")
    private String collectionType;

    @Alias("ImageTags")
    private Map<String, String> imageTags;

    @Alias("BackdropImageTags")
    private List<String> backdropImageTags;

    @Alias("LockedFields")
    private List<String> lockedFields;

    @Alias("LockData")
    private Boolean lockData;

}