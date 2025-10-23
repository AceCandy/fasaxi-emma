package cn.acecandy.fasaxi.emma.common.resp;

import cn.hutool.v7.core.map.MapUtil;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * emby缓存响应
 *
 * @author tangningzhu
 * @since 2025/4/16
 */
@Data
public class EmbyCachedResp implements Serializable {
    /**
     * 状态码
     */
    private Integer statusCode;
    /**
     * 请求头
     */
    private Map<String, String> headers = MapUtil.newHashMap();
    /**
     * 内容
     */
    private byte[] content;
}