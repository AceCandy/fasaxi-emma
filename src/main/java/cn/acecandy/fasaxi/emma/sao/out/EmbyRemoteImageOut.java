package cn.acecandy.fasaxi.emma.sao.out;

import lombok.Builder;
import lombok.Data;
import org.dromara.hutool.core.annotation.Alias;

import java.util.List;

/**
 * emby 远程图片输出类
 *
 * @author tangningzhu
 * @since 2025/4/16
 */
@Data
@Builder
public class EmbyRemoteImageOut {
    /**
     * 图片
     */
    @Alias("Images")
    private List<Img> images;

    /**
     * 图像信息
     *
     * @author AceCandy
     * @since 2025/05/03
     */
    @Builder
    @Data
    public static class Img {
        /**
         * 网址
         */
        @Alias("Url")
        private String url;
        /**
         * 缩略图url
         */
        @Alias("ThumbnailUrl")
        private String thumbnailUrl;
        /**
         * 高度
         */
        @Alias("Height")
        private Integer height;
        /**
         * 宽度
         */
        @Alias("Width")
        private Integer width;
        /**
         * 语言 en
         */
        @Alias("Language")
        private String language;
        /**
         * 显示语言 English
         */
        @Alias("DisplayLanguage")
        private String displayLanguage;
        /**
         * 类型
         * #EmbyPicType
         */
        @Alias("Type")
        private String type;
    }
}