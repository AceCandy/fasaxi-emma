package cn.acecandy.fasaxi.emma.sao.out;

import cn.hutool.v7.core.annotation.Alias;
import lombok.Data;

/**
 * 媒体资源
 *
 * @author tangningzhu
 * @since 2025/5/6
 */
@Data
public class EmbyMediaSource {
    /**
     * 媒体资源id
     */
    @Alias("Id")
    private String mediaResourceId;
    /**
     * 真实路径
     * 比如strm文件中是url
     */
    @Alias("Path")
    private String path;
    /**
     * 名称（版本）
     */
    @Alias("Name")
    private String name;
    /**
     * 容器
     * 为strm时应该是未获取到媒体信息 获取到后会是对应真实的媒体类型比如mp4/mkv等等
     */
    @Alias("Container")
    private String container;
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