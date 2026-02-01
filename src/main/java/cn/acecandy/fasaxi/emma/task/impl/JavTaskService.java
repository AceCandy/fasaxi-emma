package cn.acecandy.fasaxi.emma.task.impl;

import cn.acecandy.fasaxi.emma.common.ex.BaseException;
import cn.acecandy.fasaxi.emma.dao.embyboss.entity.JavRank;
import cn.acecandy.fasaxi.emma.dao.embyboss.service.JavRankDao;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.entity.MatchedItem;
import cn.acecandy.fasaxi.emma.utils.DateUtil;
import cn.acecandy.fasaxi.emma.utils.HtmlUtil;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.core.date.DateFormatPool;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.http.client.Request;
import cn.hutool.v7.http.client.Response;
import cn.hutool.v7.http.client.engine.ClientEngine;
import cn.hutool.v7.http.meta.Method;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * Jav任务相关 实现
 *
 * @author tangningzhu
 * @since 2025/3/3
 */
@Slf4j
@Component
public class JavTaskService {

    @Resource
    private JavRankDao javRankDao;

    @Resource
    private RedisClient redisClient;

    @Resource
    private ClientEngine httpClient;

    public static final String JAVDB_CENSORED_MONTHLY_TYPE = "JavDB有码-月榜";
    public static final String JAVDB_CENSORED_MONTHLY_URL = "http://192.168.1.249:11200/javdb/rankings/censored/monthly?limit=20";

    /**
     * 同步项目信息（从当前的source_id的最大值+1开始）
     *
     */
    public void syncJavDbCensoredMonth() {
        String today = DateFormatPool.PURE_DATE_FORMAT.format(DateUtil.today());
        JavRank javRank = javRankDao.findByTypeAndDate(JAVDB_CENSORED_MONTHLY_TYPE, today);
        if (null != javRank) {
            return;
        }
        List<String> serList = ListUtil.of();
        try (Response res = httpClient.send(Request.of(JAVDB_CENSORED_MONTHLY_URL).method(Method.GET)
                .header("User-Agent", HtmlUtil.randomUserAgent()))) {
            if (!res.isOk()) {
                throw new BaseException(StrUtil.format("返回码异常: {}", res.getStatus()));
            }
            String resBody = res.bodyStr();
            List<MatchedItem.JavDb> javItems = HtmlUtil.parseJavDbRss(resBody);
            if (CollUtil.isEmpty(javItems)) {
                return;
            }
            serList = javItems.stream().map(MatchedItem.JavDb::title).toList();
        } catch (Exception e) {
            throw new BaseException(StrUtil.format(
                    "获取JavDB Rss数据异常, url: {} ", JAVDB_CENSORED_MONTHLY_URL, e));
        }

        JavRank.x().setRankType(JAVDB_CENSORED_MONTHLY_TYPE).setSerList(serList).setRankDate(today).save();
        log.warn("[同步 {}]至DB中，当前时间:{}", JAVDB_CENSORED_MONTHLY_TYPE, today);
    }

}