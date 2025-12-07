package cn.acecandy.fasaxi.emma.sao.proxy;

import cn.acecandy.fasaxi.emma.common.ex.BaseException;
import cn.acecandy.fasaxi.emma.config.MaoyanConfig;
import cn.acecandy.fasaxi.emma.sao.out.DataEyeContent;
import cn.acecandy.fasaxi.emma.utils.HtmlUtil;
import cn.hutool.v7.core.date.DateFormatPool;
import cn.hutool.v7.core.date.DateUtil;
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
import org.jsoup.helper.DataUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


/**
 * 猫眼 代理服务
 *
 * @author AceCandy
 * @since 2025/5/3
 */
@Component
@Slf4j
public class DataEyeProxy {

    @Resource
    private ClientEngine httpClient;

    public static final String DATA_EYE_HOST =
            "https://playlet-applet.dataeye.com/playlet/listHotRanking?pageId=1&pageSize=30&month=";

    /**
     * 获得电影票房榜单
     *
     * @return {@link JSONArray }
     */
    public List<DataEyeContent> getTop30() {
        String url = DATA_EYE_HOST + DateFormatPool.NORM_MONTH_FORMAT.format(DateUtil.lastMonth());
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

            return JSONUtil.toList(resJn.getJSONArray("content"), DataEyeContent.class);
        } catch (Exception e) {
            log.warn("getTop30 网络请求异常: ", e);
        }
        return null;
    }

}