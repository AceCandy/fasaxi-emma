package cn.acecandy.fasaxi.emma.sao.proxy;

import cn.acecandy.fasaxi.emma.sao.client.DataEyeClient;
import cn.acecandy.fasaxi.emma.sao.out.DataEye;
import cn.acecandy.fasaxi.emma.sao.out.DataEyeItem;
import cn.acecandy.fasaxi.emma.sao.out.DataEyeRank;
import cn.hutool.v7.core.date.DateFormatPool;
import cn.hutool.v7.core.date.DateUtil;
import cn.hutool.v7.core.exception.ValidateException;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.json.JSONArray;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;


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
    private DataEyeClient dataEyeClient;

    /**
     * 获得电影票房榜单
     *
     * @return {@link JSONArray }
     */
    public List<DataEyeRank> getHotRankByMonth(String month) {
        if (StrUtil.isBlank(month)) {
            month = DateFormatPool.NORM_MONTH_FORMAT.format(DateUtil.lastMonth());
        }
        DataEye<List<DataEyeRank>> resp = null;
        try {
            resp = dataEyeClient.getHotRankUrl(null, month);
        } catch (Exception e) {
            log.warn("getHotRankByMonth 网络请求异常: ", e);
        }
        if (resp == null || !resp.isOk()) {
            throw new ValidateException(StrUtil.format(" getHotRankByMonth 请求异常:{}", resp));
        }
        return resp.getContent();
    }

    /**
     * 获得电影票房榜单
     *
     * @return {@link JSONArray }
     */
    public DataEyeItem getItemInfo(Long playletId) {
        if (playletId == null) {
            throw new ValidateException(StrUtil.format(" playletId 不能为空"));
        }
        DataEye<DataEyeItem> resp = null;
        try {
            resp = dataEyeClient.getItemInfo(playletId);
        } catch (Exception e) {
            log.warn("getItemInfo 网络请求异常: ", e);
        }
        if (resp == null || !resp.isOk()) {
            throw new ValidateException(StrUtil.format(" getItemInfo 请求异常:{}", resp));
        }
        return resp.getContent();
    }

}