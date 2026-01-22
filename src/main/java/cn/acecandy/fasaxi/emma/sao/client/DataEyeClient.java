package cn.acecandy.fasaxi.emma.sao.client;

import cn.acecandy.fasaxi.emma.sao.config.LogFeignConfig;
import cn.acecandy.fasaxi.emma.sao.config.RandomUaFeignConfig;
import cn.acecandy.fasaxi.emma.sao.out.DataEye;
import cn.acecandy.fasaxi.emma.sao.out.DataEyeItem;
import cn.acecandy.fasaxi.emma.sao.out.DataEyeRank;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * dataEye client
 *
 * @author tangningzhu
 * @since 2025/8/15
 */
@FeignClient(name = "DataEyeClient", url = "${dataeye.host}",
        configuration = {LogFeignConfig.class, RandomUaFeignConfig.class})
public interface DataEyeClient {


    /**
     * 获取item详情
     *
     * @param playletId 小游戏id
     * @return {@link DataEye }<{@link DataEyeItem }>
     */
    @GetMapping(path = "${dataeye.item-url}")
    DataEye<DataEyeItem> getItemInfo(@RequestParam("playletId") Long playletId);


    /**
     * 获取热力榜
     *
     * @param day   日
     * @param month 月
     * @return {@link DataEye }<{@link DataEyeItem }>
     */
    @GetMapping(path = "${dataeye.hot-rank-url}")
    DataEye<List<DataEyeRank>> getHotRankUrl(@RequestParam("day") String day,
                                             @RequestParam("month") String month);


}