package cn.acecandy.fasaxi.emma.sao.out;

import cn.hutool.v7.core.annotation.Alias;
import lombok.Data;

/**
 * emby item信息输出类
 *
 * @author tangningzhu
 * @since 2025/4/16
 */
@Data
public class EmbyImgTag {

    /**
     * 封面
     */
    @Alias("Primary")
    private String primary;
    /**
     * 封面
     */
    @Alias("Logo")
    private String logo;
    /**
     * 缩略图
     */
    @Alias("Thumb")
    private String thumb;
}