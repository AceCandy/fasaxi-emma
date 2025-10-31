package cn.acecandy.fasaxi.emma.sao.proxy;

import cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType;
import cn.acecandy.fasaxi.emma.common.ex.BaseException;
import cn.acecandy.fasaxi.emma.config.DoubanConfig;
import cn.acecandy.fasaxi.emma.sao.entity.MatchedItem;
import cn.acecandy.fasaxi.emma.sao.out.TmdbImageInfoOut;
import cn.acecandy.fasaxi.emma.utils.HtmlUtil;
import cn.acecandy.fasaxi.emma.utils.ReUtil;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.core.map.MapUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.core.text.UnicodeUtil;
import cn.hutool.v7.core.util.RandomUtil;
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

    private static final int MAX_PAGES = 50;
    private static final int ITEMS_PER_PAGE = 25;

    /**
     * 获取豆列
     *
     * @param url url
     * @return {@link String }
     */
    public List<MatchedItem.Doulist> getAllDoulist(String url) {
        if (StrUtil.isBlank(url)) {
            return null;
        }
        List<MatchedItem.Doulist> allItems = ListUtil.of();
        try {
            for (int page = 0; page < MAX_PAGES; page++) {
                int currentStart = page * ITEMS_PER_PAGE;
                String pageUrl = StrUtil.format("{}?start={}&sort=seq&playable=0&sub_type=",
                        url, currentStart);
                log.info("[豆列数据获取]➜ 正在获取第 {} 页: {}", (page + 1), pageUrl);

                try (Response res = httpClient.send(Request.of(pageUrl).method(Method.GET)
                        .form(Map.of("start", currentStart, "sort", "seq",
                                "playable", 0))
                        .header("User-Agent", HtmlUtil.randomUserAgent()))) {
                    if (!res.isOk()) {
                        throw new BaseException(StrUtil.format("返回码异常: {}", res.getStatus()));
                    }
                    String resBody = res.bodyStr();
                    List<MatchedItem.Doulist> doulistItems = HtmlUtil.parseDoulist(resBody);
                    // 如果当前页没有找到任何条目，说明到达了最后一页
                    if (CollUtil.isEmpty(doulistItems)) {
                        break;
                    }
                    allItems.addAll(doulistItems);
                } catch (Exception e) {
                    throw new BaseException(StrUtil.format("获取豆列数据异常, url: {} ", pageUrl, e));
                }
            }
            return allItems;
        } catch (Exception e) {
            log.warn("getAllDoulist 网络请求异常: ", e);
        }
        return null;
    }

    /**
     * 获取豆瓣rss
     *
     * @param url url
     * @return {@link String }
     */
    public List<MatchedItem.Doulist> getDoubanRss(String url) {
        if (StrUtil.isBlank(url)) {
            return null;
        }
        log.info("[豆瓣RSS数据获取]➜ 正在获取: {}", url);

        try (Response res = httpClient.send(Request.of(url).method(Method.GET))) {
            if (!res.isOk()) {
                throw new BaseException(StrUtil.format("返回码异常: {}", res.getStatus()));
            }
            String resBody = res.bodyStr();
            return HtmlUtil.parseDoubanRss(resBody);
        } catch (Exception e) {
            log.warn("getDoubanRss 网络请求异常: ", e);
        }
        return null;
    }


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
        String apiKey = CollUtil.get(apiKeyList, (doubanId.hashCode() & Integer.MAX_VALUE) % 2);
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
        if ((imdbId.hashCode() & Integer.MAX_VALUE) % 2 == 0) {
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
        String apiKey = CollUtil.get(apiKeyList, (imdbId.hashCode() & Integer.MAX_VALUE) % 2);
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
            if (!res.isOk() && res.getStatus() != 302) {
                throw new BaseException(StrUtil.format("返回码异常: {}", res.getStatus()));
            }
            return res.bodyStr();
        } catch (Exception e) {
            log.warn("getHtmlByImdbId 网络请求异常: ", e);
        }
        return null;
    }
}