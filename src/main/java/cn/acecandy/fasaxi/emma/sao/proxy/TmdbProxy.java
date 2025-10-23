package cn.acecandy.fasaxi.emma.sao.proxy;

import cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType;
import cn.acecandy.fasaxi.emma.common.ex.BaseException;
import cn.acecandy.fasaxi.emma.config.TmdbConfig;
import cn.acecandy.fasaxi.emma.sao.out.TmdbImageInfoOut;
import cn.hutool.v7.core.map.MapUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.http.client.Request;
import cn.hutool.v7.http.client.Response;
import cn.hutool.v7.http.client.engine.ClientEngine;
import cn.hutool.v7.http.meta.Method;
import cn.hutool.v7.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * tmdb 代理服务
 *
 * @author AceCandy
 * @since 2025/5/3
 */
@Component
@Slf4j
public class TmdbProxy {

    @Resource
    private ClientEngine httpClient;

    @Resource
    private TmdbConfig tmdbConfig;

    /**
     * 获取tmdb信息
     *
     * @param type   类型
     * @param tmdbId tmdb id
     * @return {@link TmdbImageInfoOut }
     */
    public String getInfoById(EmbyMediaType type, String tmdbId) {
        if (null == type || StrUtil.isBlank(tmdbId)) {
            return null;
        }
        String url = tmdbConfig.getHost() + StrUtil.format(tmdbConfig.getDetailInfoUrl(),
                type.getTmdbName(), tmdbId);
        try (Response res = httpClient.send(Request.of(url).method(Method.GET)
                .form(MapUtil.<String, Object>builder("api_key", tmdbConfig.getApiKey())
                        .put("append_to_response", "images,external_ids,credits,videos")
                        .put("language", "zh-CN").put("include_image_language", "zh-CN,zh,null")
                        .map()))) {
            if (!res.isOk()) {
                throw new BaseException(StrUtil.format("返回码异常: {}", res.getStatus()));
            }
            String resBody = res.bodyStr();
            if (!JSONUtil.isTypeJSON(resBody)) {
                throw new BaseException(StrUtil.format("返回结果异常: {}", resBody));
            }
            return resBody;
        } catch (Exception e) {
            log.warn("getInfoById 网络请求异常: ", e);
        }
        return null;
    }

    /**
     * 获取图片信息
     *
     * @param mediaSourceId 媒体源id
     * @return {@link TmdbImageInfoOut }
     */
    /*public TmdbImageInfoOut getImageInfo(Long mediaSourceId) {
        try (Response res = httpClient.send(Request.of(tmdbConfig.getHost() + tmdbConfig.getImageInfoUrl()).method(Method.GET)
                .form(MapUtil.<String, Object>builder("api_key", tmdbConfig.getApiKey()).map()))) {
            if (!res.isOk()) {
                throw new BaseException(StrUtil.format("返回码异常: {}", res.getStatus()));
            }
            String resBody = res.bodyStr();
            if (!JSONUtil.isTypeJSON(resBody)) {
                throw new BaseException(StrUtil.format("返回结果异常: {}", resBody));
            }
            return JSONUtil.toBean(resBody, TmdbImageInfoOut.class);
        } catch (Exception e) {
            log.warn("sendDing 网络请求异常: ", e);
        }
        return null;
    }*/


}