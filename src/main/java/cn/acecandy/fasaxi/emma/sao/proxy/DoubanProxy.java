package cn.acecandy.fasaxi.emma.sao.proxy;

import cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType;
import cn.acecandy.fasaxi.emma.common.ex.BaseException;
import cn.acecandy.fasaxi.emma.config.DoubanConfig;
import cn.acecandy.fasaxi.emma.sao.out.TmdbImageInfoOut;
import cn.acecandy.fasaxi.emma.utils.ReUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.lang.Console;
import org.dromara.hutool.core.map.MapUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.http.HttpUtil;
import org.dromara.hutool.http.client.Request;
import org.dromara.hutool.http.client.Response;
import org.dromara.hutool.http.client.engine.ClientEngine;
import org.dromara.hutool.http.meta.Method;
import org.dromara.hutool.json.JSONUtil;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 豆瓣 代理服务
 *
 * @author AceCandy
 * @since 2025/5/3
 */
@Component
@Slf4j
public class DoubanProxy {

    @Resource
    private ClientEngine httpClient;

    @Resource
    private DoubanConfig doubanConfig;


    /**
     * 通过imdb id获取豆瓣id
     *
     * @param type   类型
     * @param imdbId IMDb ID
     * @return {@link String }
     */
    public String getDoubanIdByImdbId(Integer type, String imdbId) {
        if (null == type || StrUtil.isBlank(imdbId)) {
            return null;
        }
        String doubanId = null;
        if (imdbId.hashCode() % 2 == 0) {
            doubanId = ReUtil.findDouBanIdByJson(getInfoByImdbId(type, imdbId));
        } else {
            doubanId = ReUtil.findDouBanIdByHtml(getHtmlByImdbId(imdbId));
        }
        return doubanId;
    }


    /**
     * 通过imdbid获取豆瓣信息
     *
     * @param type   类型
     * @param imdbId IMDb ID
     * @return {@link TmdbImageInfoOut }
     */
    public String getInfoByImdbId(Integer type, String imdbId) {
        String url = StrUtil.format("{}/v2/{}/imdb/{}", doubanConfig.getApp().getHost(),
                EmbyMediaType.fromCode(type).getDoubanName(), imdbId);
        List<String> apiKeyList = doubanConfig.getApp().getApikey();
        String apiKey = CollUtil.get(apiKeyList, imdbId.hashCode() % 2);
        try (Response res = httpClient.send(Request.of(url).form(MapUtil.<String, Object>builder(
                "apikey", apiKey).map()).method(Method.POST))) {
            if (!res.isOk()) {
                throw new BaseException(StrUtil.format("返回码异常: {}", res.getStatus()));
            }
            String resBody = res.bodyStr();
            if (!JSONUtil.isTypeJSON(resBody)) {
                throw new BaseException(StrUtil.format("返回结果异常: {}", resBody));
            }
            return resBody;
        } catch (Exception e) {
            log.warn("getInfoByImdbId 网络请求异常: ", e);
        }
        return null;
    }

    /**
     * 通过imdbid获取豆瓣页面
     *
     * @param imdbId IMDb ID
     * @return {@link TmdbImageInfoOut }
     */
    public String getHtmlByImdbId(String imdbId) {
        String url = StrUtil.format(
                "https://movie.douban.com/subject_search?search_text={}&cat=1002", imdbId);
        try (Response res = httpClient.send(Request.of(url).method(Method.GET))) {
            if (!res.isOk()) {
                throw new BaseException(StrUtil.format("返回码异常: {}", res.getStatus()));
            }
            return res.bodyStr();
        } catch (Exception e) {
            log.warn("getHtmlByImdbId 网络请求异常: ", e);
        }
        return null;
    }


    public static void main(String[] args) {
        String imdbId = "tt6718170"; // Example IMDb ID for "The Shawshank Redemption"
        String doubanUrl = StrUtil.format("https://api.douban.com/v2/movie/imdb/tt6718170");

        String url = "https://api.douban.com/v2/movie/imdb/tt6718170";

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("apikey", "0ac44ae016490db2204ce0a042db2916");
        String result = HttpUtil.post(url, paramMap);
        Console.log(result);
        Console.log(ReUtil.findDouBanIdByJson(result));
    }
}