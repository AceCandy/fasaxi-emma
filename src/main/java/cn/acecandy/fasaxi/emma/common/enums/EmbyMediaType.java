package cn.acecandy.fasaxi.emma.common.enums;

import cn.acecandy.fasaxi.emma.common.ex.BaseException;
import cn.hutool.v7.core.text.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * emby 媒体类型
 *
 * @author AceCandy
 * @since 2025/5/3
 */
@AllArgsConstructor
@Getter
public enum EmbyMediaType {
    // 接口出的类型
    电影(1, "Movie", "movie", "movie"),
    电视剧(2, "Series", "tv", "tv"),
    电视剧_季(3, "Season", "", ""),
    电视剧_集(4, "Episode", "", ""),
    音乐(10, "Audio", "", ""),
    人员(11, "", "person", ""),
    // 书籍(11,"Audio", "",""),
    文件夹(20, "Folder", "", ""),
    集合文件夹(50, "CollectionFolder", "", ""),
    ;

    private final Integer code;
    private final String embyName;
    private final String tmdbName;
    private final String doubanName;

    public static EmbyMediaType fromCode(Integer code) {
        for (EmbyMediaType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new BaseException("不存在的EmbyMediaType");
    }

    public static EmbyMediaType fromEmby(String embyName) {
        for (EmbyMediaType type : values()) {
            if (StrUtil.equals(type.getEmbyName(), embyName)) {
                return type;
            }
        }
        throw new BaseException("不存在的EmbyMediaType");
    }
}