package cn.acecandy.fasaxi.emma.sao.client;

import cn.acecandy.fasaxi.emma.common.ex.BaseException;
import cn.acecandy.fasaxi.emma.sao.out.R123;
import cn.acecandy.fasaxi.emma.sao.out.R123TokenReq;
import cn.acecandy.fasaxi.emma.sao.out.R123TokenResp;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.reflect.TypeReference;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.http.client.Request;
import org.dromara.hutool.http.client.Response;
import org.dromara.hutool.http.client.engine.ClientEngine;
import org.dromara.hutool.http.meta.Method;
import org.dromara.hutool.json.JSONUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 123 client
 *
 * @author tangningzhu
 * @since 2025/8/15
 */
@Component
@Slf4j
public class R123Client {

    @Resource
    private ClientEngine httpClient;

    public R123TokenResp getAccessToken(@RequestBody R123TokenReq req){
        String url = "https://open-api.123pan.com/api/v1/access_token";
        try (Response res = httpClient.send(Request.of(url).method(Method.POST)
                        .header("Platform","open_platform")
                .body(JSONUtil.toJsonStr(req)))) {
            if (!res.isOk()) {
                throw new BaseException(StrUtil.format("返回码异常[{}]: {}", res.getStatus(), url));
            }
            String resBody = res.bodyStr();
            if (!JSONUtil.isTypeJSON(resBody)) {
                throw new BaseException(StrUtil.format("返回结果异常[{}]: {}", url, resBody));
            }
            // 转为R123<R123TokenResp>
            R123<R123TokenResp> r123 = JSONUtil.toBean(resBody, new TypeReference<R123<R123TokenResp>>() {
            });
            if(!r123.isOk()){
                throw new BaseException(StrUtil.format("返回结果异常[{}]: {}", url, r123.getMessage()));
            }
            return r123.getData();
        } catch (Exception e) {
            log.warn("getItemInfo 网络请求异常: ", e);
        }
        return null;
    }
}