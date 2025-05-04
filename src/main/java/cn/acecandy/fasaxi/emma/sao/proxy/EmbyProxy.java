package cn.acecandy.fasaxi.emma.sao.proxy;

import cn.acecandy.fasaxi.emma.common.enums.EmbyPicType;
import cn.acecandy.fasaxi.emma.common.ex.BaseException;
import cn.acecandy.fasaxi.emma.config.EmbyConfig;
import cn.acecandy.fasaxi.emma.sao.out.EmbyItemInfoOut;
import cn.acecandy.fasaxi.emma.sao.out.EmbyRemoteImageOut;
import cn.acecandy.fasaxi.emma.sao.out.TmdbImageInfoOut;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.map.MapUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.http.client.Request;
import org.dromara.hutool.http.client.Response;
import org.dromara.hutool.http.client.engine.ClientEngine;
import org.dromara.hutool.http.meta.Method;
import org.dromara.hutool.json.JSONUtil;
import org.springframework.stereotype.Component;

/**
 * emby 代理服务
 *
 * @author AceCandy
 * @since 2025/5/3
 */
@Component
@Slf4j
public class EmbyProxy {

    @Resource
    private ClientEngine httpClient;

    @Resource
    private EmbyConfig embyConfig;

    /**
     * 获取项目信息
     *
     * @param mediaSourceId 媒体源id
     * @return {@link TmdbImageInfoOut }
     */
    public EmbyItemInfoOut.Item getItemInfo(Long mediaSourceId) {
        String url = embyConfig.getHost() + embyConfig.getItemInfoUrl();
        try (Response res = httpClient.send(Request.of(url).method(Method.GET)
                .form(MapUtil.<String, Object>builder("Fields", "Path,MediaSources,ProviderIds")
                        .put("Ids", mediaSourceId).put("Limit", 1)
                        .put("api_key", embyConfig.getApiKey()).map()))) {
            if (!res.isOk()) {
                throw new BaseException(StrUtil.format("返回码异常[{}]: {}", res.getStatus(), url));
            }
            String resBody = res.bodyStr();
            if (!JSONUtil.isTypeJSON(resBody)) {
                throw new BaseException(StrUtil.format("返回结果异常[{}]: {}", url, resBody));
            }
            return CollUtil.getFirst(JSONUtil.toBean(resBody, EmbyItemInfoOut.class).getItems());
        } catch (Exception e) {
            log.warn("getItemInfo 网络请求异常: ", e);
        }
        return null;
    }

    /**
     * 获取远程图片
     *
     * @param mediaSourceId 媒体源id
     * @return {@link EmbyRemoteImageOut }
     */
    public EmbyRemoteImageOut.Img getRemoteImage(Integer mediaSourceId, EmbyPicType picType) {
        String url = embyConfig.getHost() + StrUtil.format(embyConfig.getRemoteImagesUrl(), mediaSourceId);
        try (Response res = httpClient.send(Request.of(url).method(Method.GET)
                .form(MapUtil.<String, Object>builder("api_key", embyConfig.getApiKey()).put("Limit", 1)
                        .put("ProviderName", "TheMovieDb").put("Type", picType.getValue()).map()))) {
            if (!res.isOk()) {
                throw new BaseException(StrUtil.format("返回码异常[{}]: {}", res.getStatus(), url));
            }
            String resBody = res.bodyStr();
            if (!JSONUtil.isTypeJSON(resBody)) {
                throw new BaseException(StrUtil.format("返回结果异常[{}]: {}", url, resBody));
            }
            return CollUtil.getFirst(JSONUtil.toBean(resBody, EmbyRemoteImageOut.class).getImages());
        } catch (Exception e) {
            log.warn("getRemoteImage 网络请求异常: ", e);
        }
        return null;
    }
}