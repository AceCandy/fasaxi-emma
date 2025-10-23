package cn.acecandy.fasaxi.emma.sao.out;

import cn.hutool.v7.core.annotation.Alias;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * tmdb 图片信息输出类
 *
 * @author tangningzhu
 * @since 2025/4/16
 */
@Data
@Builder
public class TmdbImageInfoOut {
    /**
     * 背景图
     */
    private List<ImageInfo> backdrops;
    /**
     * tmdb id
     */
    @Alias("id")
    private Integer tmdbId;
    /**
     * logo图
     */
    private List<ImageInfo> logos;
    /**
     * 海报图
     */
    private List<ImageInfo> posters;

    /**
     * 图像信息
     *
     * @author AceCandy
     * @since 2025/05/03
     */
    @Data
    public static class ImageInfo {
        /**
         * 宽度
         */
        private Integer width;
        /**
         * 高度
         */
        private Integer height;

        /**
         * 语言 zh代表中文
         */
        @Alias("iso_639_1")
        private String language;
        /**
         * 文件路径
         */
        @Alias("file_path")
        private String filePath;
        /**
         * 投票人数
         */
        @Alias("vote_count")
        private Integer voteCount;
        /**
         * 平均投票数
         */
        @Alias("vote_average")
        private Double voteAverage;
    }
}