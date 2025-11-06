package cn.acecandy.fasaxi.emma.dao.toolkit.dto;

import cn.hutool.v7.core.annotation.Alias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 自定义虚拟库中的媒体信息
 *
 * @author tangningzhu
 * @since 2025/11/5
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GeneratedMediaInfo {
    // String title, String status, String emby_id, String tmdb_id,
    // String poster_path, String release_date, Integer season;

    /**
     * 标题
     */
    private String title;
    /**
     * 状态
     */
    private String status;
    /**
     * Emby ID
     */
    @Alias("emby_id")
    @JsonProperty("emby_id")
    private String embyId;
    /**
     * TMDB ID
     */
    @Alias("tmdb_id")
    @JsonProperty("tmdb_id")
    private String tmdbId;
    /**
     * 海报路径
     */
    @Alias("poster_path")
    @JsonProperty("poster_path")
    private String posterPath;
    /**
     * 发布日期
     */
    @Alias("release_date")
    @JsonProperty("release_date")
    private String releaseDate;
    /**
     * 季数
     */
    private Integer season;

}