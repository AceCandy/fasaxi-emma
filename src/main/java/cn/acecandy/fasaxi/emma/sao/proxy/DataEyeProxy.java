package cn.acecandy.fasaxi.emma.sao.proxy;

import cn.acecandy.fasaxi.emma.sao.client.DataEyeClient;
import cn.acecandy.fasaxi.emma.sao.out.DataEye;
import cn.acecandy.fasaxi.emma.sao.out.DataEyeItem;
import cn.acecandy.fasaxi.emma.sao.out.DataEyeRank;
import cn.hutool.v7.core.exception.ValidateException;
import cn.hutool.v7.core.text.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * 猫眼 代理服务
 *
 * @author AceCandy
 * @since 2025/5/3
 */
@Component
@Slf4j
public class DataEyeProxy {

    private static final int MONTH_LENGTH = 7;
    private static final int DAY_LENGTH = 10;

    @Resource
    private DataEyeClient dataEyeClient;


    /**
     * 获得电影票房榜单
     *
     * @return {@link JSONArray }
     */
    public List<DataEyeRank> getHotRank(String timeValue) {
        return getRankByDayOrMonth("getHotRank", timeValue, dataEyeClient::getHotRankUrl);
    }

    /**
     * 获得抖音票房榜单
     *
     * @return {@link JSONArray }
     */
    public List<DataEyeRank> getDyRank(String timeValue) {
        return getRankByDayOrMonth("getDyRank", timeValue, dataEyeClient::getDyUrl);
    }

    /**
     * 获得快手热播榜单
     *
     * @return {@link JSONArray }
     */
    public List<DataEyeRank> getKsRank(String timeValue) {
        return getRankByDayOrMonth("getKsRank", timeValue, dataEyeClient::getKsUrl);
    }

    /**
     * 获得红果热播榜单
     *
     * @return {@link JSONArray }
     */
    public List<DataEyeRank> getHgRank(String timeValue) {
        return getRankByDayOrMonth("getHgRank", timeValue, dataEyeClient::getHgUrl);
    }

    /**
     * 获得品牌热播榜单
     *
     * @return {@link JSONArray }
     */
    public List<DataEyeRank> getBrandRank(String timeValue) {
        return getRankByDayOrMonth("getBrandRank", timeValue, dataEyeClient::getBrandUrl);
    }

    /**
     * 获得爱奇艺热播榜单
     *
     * @return {@link JSONArray }
     */
    public List<DataEyeRank> getIqyRank(String timeValue) {
        return getRankByDay("getIqyRank", timeValue, dataEyeClient::getIqyUrl);
    }

    /**
     * 获得优酷热播榜单
     *
     * @return {@link JSONArray }
     */
    public List<DataEyeRank> getYkRank(String timeValue) {
        return getRankByDay("getYkRank", timeValue, dataEyeClient::getYkUrl);
    }

    /**
     * 获得腾讯竖剧热播榜单
     *
     * @return {@link JSONArray }
     */
    public List<DataEyeRank> getTxsRank(String timeValue) {
        return getRankByDay("getTxsRank", timeValue, dataEyeClient::getTxsUrl);
    }

    /**
     * 获得腾讯横剧热播榜单
     *
     * @return {@link JSONArray }
     */
    public List<DataEyeRank> getTxhRank(String timeValue) {
        return getRankByDay("getTxhRank", timeValue, dataEyeClient::getTxhUrl);
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
        String keyInfo = StrUtil.format("playletId={}", playletId);
        return executeRequest("getItemInfo", keyInfo, () -> dataEyeClient.getItemInfo(playletId));
    }

    /**
     * 统一处理日/月榜单请求
     */
    private List<DataEyeRank> getRankByDayOrMonth(String methodName, String timeValue,
                                                  BiFunction<String, String, DataEye<List<DataEyeRank>>> requester) {
        validateTimeValueNotBlank(timeValue);
        String keyInfo = StrUtil.format("timeValue={}", timeValue);
        if (StrUtil.length(timeValue) == MONTH_LENGTH) {
            return executeRequest(methodName, keyInfo, () -> requester.apply(null, timeValue));
        }
        if (StrUtil.length(timeValue) == DAY_LENGTH) {
            return executeRequest(methodName, keyInfo, () -> requester.apply(timeValue, null));
        }
        return executeRequest(methodName, keyInfo, () -> null);
    }

    /**
     * 统一处理仅日维度的榜单请求
     */
    private List<DataEyeRank> getRankByDay(String methodName, String timeValue,
                                           Function<String, DataEye<List<DataEyeRank>>> requester) {
        validateTimeValueNotBlank(timeValue);
        if (StrUtil.length(timeValue) != DAY_LENGTH) {
            return null;
        }

        String keyInfo = StrUtil.format("timeValue={}", timeValue);
        return executeRequest(methodName, keyInfo, () -> requester.apply(timeValue));
    }

    /**
     * 校验时间参数不能为空
     */
    private void validateTimeValueNotBlank(String timeValue) {
        if (StrUtil.isBlank(timeValue)) {
            throw new ValidateException(StrUtil.format(" timeValue 不能为空"));
        }
    }

    /**
     * 执行请求并统一处理返回
     */
    private <T> T executeRequest(String methodName, String keyInfo, Supplier<DataEye<T>> request) {
        DataEye<T> resp = null;
        boolean hasException = false;
        try {
            resp = request.get();
        } catch (Exception e) {
            hasException = true;
            log.warn("{} 网络请求异常, {}", methodName, keyInfo, e);
        }
        if (resp == null || !resp.isOk()) {
            if (!hasException) {
                log.warn("{} 请求异常, {}, resp={}", methodName, keyInfo, resp);
            }
            throw new ValidateException(StrUtil.format(" {} 请求异常:{}, {}", methodName, resp, keyInfo));
        }
        return resp.getContent();
    }
}