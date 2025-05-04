package cn.acecandy.fasaxi.emma.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * emby 图片类型
 *
 * @author AceCandy
 * @since 2025/5/3
 */
@AllArgsConstructor
@Getter
public enum EmbyPicType {
    // 接口出的类型
    非图片("NoPic", 0),
    封面("Primary", 1),
    背景图("Backdrop", 2),
    Logo("Logo", 3),
    缩略图("Thumb", 4),
    横幅("Banner", 5),
    ;

    private final String value;
    private final Integer type;
}