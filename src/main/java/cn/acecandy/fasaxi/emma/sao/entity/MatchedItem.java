package cn.acecandy.fasaxi.emma.sao.entity;

import cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType;

/**
 * rss任务中匹配的结构
 *
 * @author tangningzhu
 * @since 2025/10/31
 */
public record MatchedItem(Integer id, String name, EmbyMediaType type) {
    public record Doulist(String title, Integer year, String doubanLink, String doubanId, String imdbId) {
    }

    public record FilterRule(String field, String value, String operator) {
    }

    public record FilterItem(String tmdbId, String itemType) {
    }

    //"order": 0, "tmdb_id": 5052399, "character": "饰 穆萨"
    public record FieldPerson(String tmdb_id) {
    }

    public record ValuePerson(String department, String id, String known_for,
                              String name, String profile_path) {
    }
}