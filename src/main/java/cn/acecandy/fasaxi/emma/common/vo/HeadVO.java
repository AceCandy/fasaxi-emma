package cn.acecandy.fasaxi.emma.common.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 请求头 VO
 *
 * @author tangningzhu
 * @since 2024/11/22
 */
@Builder
@Data
public class HeadVO {
    private String ua;
}