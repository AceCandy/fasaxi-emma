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
import org.dromara.hutool.core.text.UnicodeUtil;
import org.dromara.hutool.core.util.RandomUtil;
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
     * 按id获取豆瓣配置
     *
     * @param type     类型
     * @param doubanId Douban ID
     * @return {@link String }
     */
    public String getInfoById(EmbyMediaType type, String doubanId) {
        if (null == type || StrUtil.isBlank(doubanId)) {
            return null;
        }
        String url = doubanConfig.getWxmini().getHost() +
                StrUtil.format(doubanConfig.getWxmini().getDetailInfoUrl(),
                        type.getDoubanName(), doubanId);
        List<String> apiKeyList = doubanConfig.getWxmini().getApikey();
        String apiKey = CollUtil.get(apiKeyList, doubanId.hashCode() % 2);
        String referer = StrUtil.format("https://servicewechat.com/wx2f9b06c1de1ccfca/{}/page-frame.html",
                RandomUtil.randomInt(15, 97));
        try (Response res = httpClient.send(Request.of(url)
                .form(MapUtil.<String, Object>builder("apikey", apiKey).map())
                .header("Referer", referer)
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36 MicroMessenger/7.0.20.1781(0x6700143B) NetType/WIFI MiniProgramEnv/Mac MacWechat/WMPF MacWechat/3.8.7(0x13080712) UnifiedPCMacWechat(0xf2640509) XWEB/13822")
                .method(Method.GET))) {
            if (!res.isOk()) {
                throw new BaseException(StrUtil.format("返回码异常: {}", res.getStatus()));
            }
            String resBody = res.bodyStr();
            if (!JSONUtil.isTypeJSON(resBody)) {
                throw new BaseException(StrUtil.format("返回结果异常: {}", resBody));
            }
            return UnicodeUtil.toString(resBody);
        } catch (Exception e) {
            log.warn("getDoubanConfigById 网络请求异常: ", e);
        }
        return null;
    }


    /**
     * 通过imdb id获取豆瓣id
     *
     * @param type   类型
     * @param imdbId IMDb ID
     * @return {@link String }
     */
    public String getDoubanIdByImdbId(EmbyMediaType type, String imdbId) {
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
    public String getInfoByImdbId(EmbyMediaType type, String imdbId) {
        if (null == type || StrUtil.isBlank(imdbId)) {
            return null;
        }
        String url = StrUtil.format("{}/v2/{}/imdb/{}", doubanConfig.getApp().getHost(),
                type.getDoubanName(), imdbId);
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
        if (StrUtil.isBlank(imdbId)) {
            return null;
        }
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