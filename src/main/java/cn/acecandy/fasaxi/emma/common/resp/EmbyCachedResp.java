package cn.acecandy.fasaxi.emma.common.resp;

import cn.hutool.core.map.MapUtil;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;

import java.util.Arrays;
import java.util.Map;

/**
 * emby缓存响应
 *
 * @author tangningzhu
 * @since 2025/4/16
 */
@Data
public class EmbyCachedResp {
    /**
     * 状态码
     */
    private int statusCode;
    /**
     * 请求头
     */
    private Map<String, String> headers = MapUtil.newHashMap();
    /**
     * 内容
     */
    private byte[] content;

    /**
     * 过期时间
     */
    private Long exTime;

    @SneakyThrows
    public static EmbyCachedResp transfer(CloseableHttpResponse backendRes) {
        EmbyCachedResp embyCachedResp = new EmbyCachedResp();
        embyCachedResp.statusCode = backendRes.getStatusLine().getStatusCode();
        Arrays.stream(backendRes.getAllHeaders())
                .forEach(h -> embyCachedResp.headers.put(h.getName(), h.getValue()));
        if (null != backendRes.getEntity()) {
            embyCachedResp.content = EntityUtils.toByteArray(backendRes.getEntity());
        }
        return embyCachedResp;
    }
}