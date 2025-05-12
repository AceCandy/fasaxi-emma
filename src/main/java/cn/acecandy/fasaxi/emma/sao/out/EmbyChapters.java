package cn.acecandy.fasaxi.emma.sao.out;

import lombok.Data;
import org.dromara.hutool.core.annotation.Alias;

/**
 * emby Chapters信息输出类
 *
 * @author tangningzhu
 * @since 2025/4/16
 */
@Data
public class EmbyChapters {

    /**
     * track索引
     */
    @Alias("StartPositionTicks")
    private Long startPositionTicks;

    /**
     * 标题
     */
    @Alias("Name")
    private String name;
    /**
     * 标记类型 Chapter IntroStart IntroEnd
     */
    @Alias("MarkerType")
    private String markerType;

    /**
     * 索引
     */
    @Alias("ChapterIndex")
    private Integer chapterIndex;

}