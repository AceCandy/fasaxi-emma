package cn.acecandy.fasaxi.emma.sao.out;

import lombok.Data;
import org.dromara.hutool.core.annotation.Alias;

import java.util.List;

/**
 * emby playback信息输出类
 *
 * @author tangningzhu
 * @since 2025/4/16
 */
@Data
public class EmbyPlaybackOut {

    /**
     * 媒体信息
     */
    @Alias("MediaSources")
    private List<EmbyMediaSource> mediaSources;

    /**
     * session标识
     */
    @Alias("PlaySessionId")
    private String playSessionId;

}