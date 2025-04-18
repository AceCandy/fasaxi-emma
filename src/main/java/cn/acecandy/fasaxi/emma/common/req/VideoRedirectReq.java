package cn.acecandy.fasaxi.emma.common.req;

import cn.hutool.core.util.StrUtil;
import lombok.Data;

/**
 * 视频重定向 入参
 *
 * @author tangningzhu
 * @since 2024/11/22
 */
@Data
public class VideoRedirectReq {
    // Key: PlaySessionId, Value: ed4a8bb915964952a525a6dd4574ff04
    // Key: api_key, Value: 523aaeef285b4db28ecb43a14223eeea
    // Key: DeviceId, Value: 8a651944-f5af-427a-b204-d59d11778495
    // Key: MediaSourceId, Value: mediasource_252661
    private String PlaySessionId;
    private String api_key;
    private String DeviceId;
    private String MediaSourceId;

    public String getMediaSourceId() {
        return StrUtil.removePrefix(MediaSourceId, "mediasource_");
    }
}