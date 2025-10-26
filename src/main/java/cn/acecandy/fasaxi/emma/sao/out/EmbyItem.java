package cn.acecandy.fasaxi.emma.sao.out;

import cn.hutool.v7.core.annotation.Alias;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * emby item信息输出类
 *
 * @author tangningzhu
 * @since 2025/4/16
 */
@Data
public class EmbyItem {

    /**
     * 服务器id
     */
    @Alias("ServerId" )
    private String serverId;

    /**
     * 标题
     */
    @Alias("Name" )
    private String name;
    /**
     * 项目id
     */
    @Alias("Id" )
    private String itemId;

    /**
     * 文件路径
     */
    @Alias("Path" )
    private String path;

    /**
     * 唯一key tmdb会在上面标识
     */
    @Alias("PresentationUniqueKey" )
    private String uniqueKey;

    /**
     * 文件名
     */
    @Alias("FileName" )
    private String fileName;

    /**
     * 类型
     * 参考 EmbyMediaType
     */
    @Alias("Type" )
    private String type;

    /**
     * 父背景图id
     */
    @Alias("ParentBackdropItemId" )
    private String parentBackdropItemId;

    /**
     * 父背景图标记
     */
    @Alias("ParentBackdropImageTags" )
    private List<String> parentBackdropImageTags;

    /**
     * 用户信息
     */
    @Alias("UserData" )
    private Map<String, Object> userData;

    /**
     * 对应的父id
     * ps：剧集上一级是季 季上一级才是剧集
     */
    @Alias("ParentId" )
    private String parentId;

    /**
     * 年份
     */
    @Alias("ProductionYear" )
    private Integer productionYear;

    /**
     * 剧集中第几季
     */
    @Alias("ParentIndexNumber" )
    private Integer parentIndexNumber;

    /**
     * 剧集中第几集
     */
    @Alias("IndexNumber" )
    private Integer indexNumber;

    /**
     * 剧集名称
     */
    @Alias("SeriesName" )
    private String seriesName;

    /**
     * 剧集id本我
     */
    @Alias("SeriesId" )
    private String seriesId;

    /**
     * 季id
     */
    @Alias("SeasonId" )
    private String seasonId;

    /**
     * 剧集封面图标记
     */
    @Alias("SeriesPrimaryImageTag" )
    private String seriesPrimaryImageTag;

    /**
     * 季名称
     */
    @Alias("SeasonName" )
    private String seasonName;

    /**
     * 媒体类型 video
     */
    @Alias("MediaType" )
    private String mediaType;


    /**
     * 是否文件夹
     */
    @Alias("IsFolder" )
    private Boolean isFolder;

    /**
     * 图像标记
     * 有图片就会有值
     */
    @Alias("PrimaryImageAspectRatio" )
    private Double primaryImageAspectRatio;

    /**
     * 图像标记
     * 有图片就会有值
     */
    @Alias("ImageTags" )
    private EmbyImgTag imageTags;

    /**
     * 背景图片标记
     */
    @Alias("BackdropImageTags" )
    private List<String> backdropImageTags;

    /**
     * 章节
     */
    @Alias("Chapters" )
    private List<EmbyChapters> chapters;

    /**
     * 容器类型 mkv、mp4
     */
    @Alias("Container" )
    private String container;
    /**
     * 媒体信息
     */
    @Alias("MediaSources" )
    private List<EmbyMediaSource> mediaSources;

    /**
     * 时长(单位为100纳秒 ) /10000/60/1000 为分钟数
     * 没有获取到媒体信息时该值为null
     */
    @Alias("RunTimeTicks" )
    private Long runTimeTicks;

    /**
     * 文件大小（单位byte） /1024/1024 为MB
     * 没有获取到媒体信息时该值为0
     */
    @Alias("Size" )
    private Long size;

    /**
     * 比特率
     * 没有获取到媒体信息时该值为null
     */
    @Alias("Bitrate" )
    private Long bitrate;

    /**
     * 外部第三方id
     * 当前有 Tmdb、Tvdb、Imdb、bilibili
     */
    @Alias("ProviderIds" )
    private Map<String, String> providerIds;

    /**
     * 是否可删除
     */
    @Alias("CanDelete" )
    private Boolean canDelete;

    /**
     * 可以下载
     */
    @Alias("CanDownload" )
    private Boolean canDownload;

    /**
     * 社区评分
     */
    @Alias("CommunityRating" )
    private Double communityRating;

    /**
     * 影评人评分
     */
    @Alias("CriticRating" )
    private Double criticRating;

    /**
     * 家长分级
     */
    @Alias("OfficialRating" )
    private String officialRating;

    @Alias("SupportsSync" )
    private Boolean supportsSync;
}