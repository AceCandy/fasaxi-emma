package cn.acecandy.fasaxi.emma.common.resp;

import cn.hutool.v7.core.annotation.Alias;
import lombok.Data;

import java.util.List;

/**
 * 视频信息 出参
 *
 * @author tangningzhu
 * @since 2024/11/22
 */
@Data
public class EmbyItemsResp {
    /**
     * emby项目
     */
    @Alias("Items")
    private List<EmbyItems> items;
    /**
     * 总记录数
     */
    @Alias("TotalRecordCount")
    private Integer totalRecordCount;

    @Data
    public static class EmbyItems {
        /**
         * 文件名称
         */
        @Alias("Name")
        private String name;
        /**
         * 文件id
         */
        @Alias("Id")
        private String id;
        /**
         * 媒体来源信息
         */
        @Alias("MediaSources")
        private List<MediaSources> mediaSources;
        /**
         * 本地路径
         */
        @Alias("Path")
        private String path;

        @Data
        public static class MediaSources {
            /**
             * strm具体路径
             */
            @Alias("Path")
            private String path;
        }
    }
}
