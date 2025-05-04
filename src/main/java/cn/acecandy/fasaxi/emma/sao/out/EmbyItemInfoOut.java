package cn.acecandy.fasaxi.emma.sao.out;

import lombok.Builder;
import lombok.Data;
import org.dromara.hutool.core.annotation.Alias;

import java.util.List;

/**
 * emby item信息输出类
 *
 * @author tangningzhu
 * @since 2025/4/16
 */
@Data
@Builder
public class EmbyItemInfoOut {

    /**
     * 项目
     */
    @Alias("Items")
    private List<Item> items;
    /**
     * 总记录数
     */
    @Alias("TotalRecordCount")
    private Integer totalRecordCount;

    @Data
    public static class Item {
        /**
         * 电影/剧集名称
         */
        @Alias("Name")
        private String name;
        /**
         * 项目Id
         */
        @Alias("Id")
        private String itemId;

        /**
         * 容器类型 mkv、mp4
         */
        @Alias("Container")
        private String container;
        /**
         * 媒体信息
         */
        @Alias("MediaSources")
        private List<MediaSource> mediaSources;

        /**
         * 本地路径
         */
        @Alias("Path")
        private String path;

        /**
         * 时长
         */
        @Alias("RunTimeTicks")
        private Long runTimeTicks;

        /**
         * 文件大小
         */
        @Alias("Size")
        private Long size;

        /**
         * 比特率
         */
        @Alias("Bitrate")
        private Long bitrate;

        /**
         * 类型 Movie
         */
        @Alias("Bitrate")
        private String type;

        /**
         * 媒体类型 video
         */
        @Alias("MediaType")
        private String mediaType;
    }

    /**
     * 图像信息
     *
     * @author AceCandy
     * @since 2025/05/03
     */
    @Data
    public static class MediaSource {
        /**
         * 媒体资源id
         */
        @Alias("Id")
        private String mediaResourceId;
        /**
         * 真实路径
         */
        @Alias("Path")
        private String path;
        /**
         * 名称（版本）
         */
        @Alias("Name")
        private String name;
        /**
         * 是远程
         */
        @Alias("IsRemote")
        private Boolean isRemote;
        /**
         * 项目id
         */
        @Alias("ItemId")
        private String itemId;
    }
}