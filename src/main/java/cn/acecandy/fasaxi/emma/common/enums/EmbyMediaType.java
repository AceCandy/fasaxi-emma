package cn.acecandy.fasaxi.emma.common.enums;

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
    电影("Movie", "movie"),
    电视剧("Episode", "tv"),
    电视剧_季("Season", ""),
    ;

    private final String value;
    private final String tmdbName;
}