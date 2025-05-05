package cn.acecandy.fasaxi.emma.common.constants;

/**
 * @author tangningzhu
 * @since 2024/11/22
 */
public interface CacheConstant {
    // 时间
    Long DAY_7_MS = 7 * 24 * 60 * 60 * 1000L;

    // http状态码

    Integer CODE_302 = 302;
    Integer CODE_404 = 404;
    Integer CODE_416 = 416;
    Integer CODE_429 = 429;
    Integer CODE_200 = 200;
    Integer CODE_204 = 204;

    // http请求方法

    String HTTP_GET = "GET";
}