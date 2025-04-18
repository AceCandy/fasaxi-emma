package cn.acecandy.fasaxi.emma.common.vo;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpHeaders;

import java.util.Map;

/**
 * 请求头 VO
 *
 * @author tangningzhu
 * @since 2024/11/22
 */
@Builder
@Data
public class HttpReqVO {
    /**
     * 请求uri
     */
    private String requestUri;

    /**
     * 方法
     */
    private String method;
    /**
     * 入参字典
     */
    private Map<String, String[]> paramsDict;
    /**
     * 请求头
     */
    private HttpHeaders headers;
    /**
     * ua
     */
    private String ua;
}