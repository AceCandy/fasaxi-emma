package cn.acecandy.fasaxi.emma.sao.proxy;

import cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType;
import cn.acecandy.fasaxi.emma.common.ex.BaseException;
import cn.acecandy.fasaxi.emma.config.TmdbConfig;
import cn.acecandy.fasaxi.emma.sao.out.RTmdb;
import cn.acecandy.fasaxi.emma.sao.out.RTmdbMovie;
import cn.acecandy.fasaxi.emma.sao.out.RTmdbTv;
import cn.acecandy.fasaxi.emma.sao.out.TmdbImageInfoOut;
import cn.hutool.v7.core.bean.BeanUtil;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.map.MapUtil;
import cn.hutool.v7.core.reflect.TypeReference;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.http.client.Request;
import cn.hutool.v7.http.client.Response;
import cn.hutool.v7.http.client.engine.ClientEngine;
import cn.hutool.v7.http.meta.Method;
import cn.hutool.v7.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType.电影;
import static cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType.电视剧;

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
     * 获取movie tmdb信息(通过名称)
     *
     * @param name 名称
     * @param year 年
     * @return {@link List }<{@link RTmdbMovie }>
     */
    public List<RTmdbMovie> getMovieByName(String name, Integer year) {
        if (StrUtil.isBlank(name)) {
            return null;
        }
        List<String> result = getInfoByName(电影, name, year);
        if (CollUtil.isEmpty(result)) {
            return null;
        }
        return BeanUtil.copyToList(result, RTmdbMovie.class);
    }

    /**
     * 获取tv tmdb信息(通过名称)
     *
     * @param name 名称
     * @param year 年
     * @return {@link List }<{@link RTmdbTv }>
     */
    public List<RTmdbTv> getTvByName(String name, Integer year) {
        if (StrUtil.isBlank(name)) {
            return null;
        }
        List<String> result = getInfoByName(电视剧, name, year);
        if (CollUtil.isEmpty(result)) {
            return null;
        }
        return BeanUtil.copyToList(result, RTmdbTv.class);
    }

    /**
     * 按名称获取信息
     *
     * @param type 类型
     * @param name 名字
     * @param year 年
     * @return {@link RTmdb }<{@link String }>
     */
    public List<String> getInfoByName(EmbyMediaType type, String name, Integer year) {
        if (StrUtil.isBlank(name)) {
            return null;
        }
        String url = tmdbConfig.getHost() + StrUtil.format(tmdbConfig.getSearchDetailInfoUrl(),
                type.getTmdbName());
        try (Response res = httpClient.send(Request.of(url).method(Method.GET)
                .form(Map.of("api_key", tmdbConfig.getApiKey(), "language", "zh-CN",
                        电影.equals(type) ? "year" : "first_air_date_year", year,
                        "include_adult", true, "query", name)))) {
            if (!res.isOk()) {
                throw new BaseException(StrUtil.format("返回码异常: {}", res.getStatus()));
            }
            String resBody = res.bodyStr();
            if (!JSONUtil.isTypeJSON(resBody)) {
                throw new BaseException(StrUtil.format("返回结果异常: {}", resBody));
            }
            RTmdb<String> resp = JSONUtil.toBean(resBody, new TypeReference<RTmdb<String>>() {
            });
            if (null == resp || resp.getTotalResults() == 0) {
                return null;
            }
            return resp.getResults();
        } catch (Exception e) {
            log.warn("getInfoByName 网络请求异常: ", e);
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