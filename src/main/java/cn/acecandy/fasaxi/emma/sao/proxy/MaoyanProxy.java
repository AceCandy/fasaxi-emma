package cn.acecandy.fasaxi.emma.sao.proxy;

import cn.acecandy.fasaxi.emma.common.ex.BaseException;
import cn.acecandy.fasaxi.emma.config.MaoyanConfig;
import cn.acecandy.fasaxi.emma.utils.HtmlUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.http.client.Request;
import cn.hutool.v7.http.client.Response;
import cn.hutool.v7.http.client.engine.ClientEngine;
import cn.hutool.v7.http.meta.Method;
import cn.hutool.v7.json.JSONArray;
import cn.hutool.v7.json.JSONObject;
import cn.hutool.v7.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;


/**
 * 猫眼 代理服务
 *
 * @author AceCandy
 * @since 2025/5/3
 */
@Component
@Slf4j
public class MaoyanProxy {

    @Resource
    private ClientEngine httpClient;

    @Resource
    private MaoyanConfig maoyanConfig;

    /**
     * 电视类型
     */
    public static final Map<String, Integer> TV_HEAT_MAP = Map.of(
            "web-heat", 0, "web-tv", 1, "zongyi", 2
    );

    /**
     * 平台代码
     */
    public static final Map<String, Integer> PLATFORM_MAP = Map.of(
            "all", 0, "tencent", 3, "iqiyi", 2, "youku", 1, "mango", 7
    );

    /**
     * 获得电影票房榜单
     *
     * @return {@link JSONArray }
     */
    public JSONArray getMovieTop() {
        String url = maoyanConfig.getHost() + maoyanConfig.getMovieUrl();
        try (Response res = httpClient.send(Request.of(url).method(Method.GET)
                .header("User-Agent", HtmlUtil.randomUserAgent()))) {
            if (!res.isOk()) {
                throw new BaseException(StrUtil.format("返回码异常: {}", res.getStatus()));
            }
            String resBody = res.bodyStr();
            if (!JSONUtil.isTypeJSON(resBody)) {
                throw new BaseException(StrUtil.format("返回结果异常: {}", resBody));
            }
            JSONObject resJn = JSONUtil.parseObj(resBody);
            return resJn.getJSONObject("movieList").getJSONArray("list");
        } catch (Exception e) {
            log.warn("getMovieTop 网络请求异常: ", e);
        }
        return null;
    }

    /**
     * 获得电视剧热度榜单
     *
     * @return {@link JSONArray }
     */
    public JSONArray getTvTop(Integer seriesType, Integer platformType) {
        String url = maoyanConfig.getHost() + maoyanConfig.getTvUrl();
        try (Response res = httpClient.send(Request.of(url).method(Method.GET)
                .header("User-Agent", HtmlUtil.randomUserAgent())
                .form(Map.of("showDate", 2,
                        "seriesType", seriesType, "platformType", platformType)))) {
            if (!res.isOk()) {
                throw new BaseException(StrUtil.format("返回码异常: {}", res.getStatus()));
            }
            String resBody = res.bodyStr();
            if (!JSONUtil.isTypeJSON(resBody)) {
                throw new BaseException(StrUtil.format("返回结果异常: {}", resBody));
            }
            JSONObject resJn = JSONUtil.parseObj(resBody);
            return resJn.getJSONObject("dataList").getJSONArray("list");
        } catch (Exception e) {
            log.warn("getTvTop 网络请求异常: ", e);
        }
        return null;
    }

}