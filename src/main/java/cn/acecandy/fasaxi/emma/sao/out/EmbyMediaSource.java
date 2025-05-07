package cn.acecandy.fasaxi.emma.sao.out;

import lombok.Data;
import org.dromara.hutool.core.annotation.Alias;

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