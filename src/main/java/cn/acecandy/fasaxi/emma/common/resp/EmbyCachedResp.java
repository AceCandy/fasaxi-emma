package cn.acecandy.fasaxi.emma.common.resp;

import cn.acecandy.fasaxi.emma.utils.CompressUtil;
import lombok.Data;
import lombok.SneakyThrows;
import org.dromara.hutool.core.map.MapUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.http.client.Response;
import org.dromara.hutool.http.client.body.ResponseBody;

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
    public static EmbyCachedResp transfer(Response res, String method) {
        EmbyCachedResp embyCachedResp = new EmbyCachedResp();
        embyCachedResp.statusCode = res.getStatus();
        res.headers().forEach((k, v) -> {
            if (k == null || StrUtil.equalsIgnoreCase(k, "content-length")) {
                return;
            }
            embyCachedResp.headers.put(k, StrUtil.join(StrUtil.COMMA, v));
        });
        ResponseBody body = res.body().sync();
        if (StrUtil.equalsAnyIgnoreCase(method, "get") && StrUtil.containsIgnoreCase(
                embyCachedResp.getHeaders().get("Content-Type"), "application/json")) {
            if (StrUtil.equalsIgnoreCase(embyCachedResp.getHeaders().get("Content-Encoding"), "br")) {
                String bodyStr = new String(CompressUtil.decode(body.getBytes()));
                String content = StrUtil.replaceIgnoreCase(bodyStr, "micu", "REDMT");
                embyCachedResp.content = content.getBytes();
            }else{
                String content = StrUtil.replaceIgnoreCase(body.getString(), "micu", "REDMT");
                embyCachedResp.content = content.getBytes();
            }
        } else {
            embyCachedResp.content = body.getBytes();
        }
        return embyCachedResp;
    }
}