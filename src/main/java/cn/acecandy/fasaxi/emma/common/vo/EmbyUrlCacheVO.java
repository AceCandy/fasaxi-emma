package cn.acecandy.fasaxi.emma.common.vo;

import lombok.Builder;
import lombok.Data;

/**
 * emby url缓存 VO
 *
 * @author tangningzhu
 * @since 2025/4/16
 */
@Data
@Builder
public class EmbyUrlCacheVO {
    /**
     * 状态码
     */
    private String url;

    /**
     * 过期时间
     */
    private Long exTime;
}