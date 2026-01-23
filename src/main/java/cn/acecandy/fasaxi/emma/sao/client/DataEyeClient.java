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

    /**
     * 获取抖音热播
     *
     * @param day   日
     * @param month 月
     * @return {@link DataEye }<{@link DataEyeItem }>
     */
    @GetMapping(path = "${dataeye.dy-url}")
    DataEye<List<DataEyeRank>> getDyUrl(@RequestParam("day") String day,
                                        @RequestParam("month") String month);

    /**
     * 获取快手热播
     *
     * @param day   日
     * @param month 月
     * @return {@link DataEye }<{@link DataEyeItem }>
     */
    @GetMapping(path = "${dataeye.ks-url}")
    DataEye<List<DataEyeRank>> getKsUrl(@RequestParam("day") String day,
                                        @RequestParam("month") String month);

    /**
     * 获取红果热播
     *
     * @param day   日
     * @param month 月
     * @return {@link DataEye }<{@link DataEyeItem }>
     */
    @GetMapping(path = "${dataeye.hg-url}")
    DataEye<List<DataEyeRank>> getHgUrl(@RequestParam("day") String day,
                                        @RequestParam("month") String month);

    /**
     * 获取品牌热播
     *
     * @param day   日
     * @param month 月
     * @return {@link DataEye }<{@link DataEyeItem }>
     */
    @GetMapping(path = "${dataeye.brand-url}")
    DataEye<List<DataEyeRank>> getBrandUrl(@RequestParam("day") String day,
                                           @RequestParam("month") String month);

    /**
     * 获取爱奇艺热播
     *
     * @param day 日
     * @return {@link DataEye }<{@link DataEyeItem }>
     */
    @GetMapping(path = "${dataeye.iqy-url}")
    DataEye<List<DataEyeRank>> getIqyUrl(@RequestParam("rankingDate") String day);


    /**
     * 获取优酷热播
     *
     * @param day 日
     * @return {@link DataEye }<{@link DataEyeItem }>
     */
    @GetMapping(path = "${dataeye.yk-url}")
    DataEye<List<DataEyeRank>> getYkUrl(@RequestParam("rankingDate") String day);

    /**
     * 获取腾讯竖剧热播
     *
     * @param day 日
     * @return {@link DataEye }<{@link DataEyeItem }>
     */
    @GetMapping(path = "${dataeye.txs-url}")
    DataEye<List<DataEyeRank>> getTxsUrl(@RequestParam("rankingDate") String day);

    /**
     * 获取腾讯横剧热播
     *
     * @param day 日
     * @return {@link DataEye }<{@link DataEyeItem }>
     */
    @GetMapping(path = "${dataeye.txh-url}")
    DataEye<List<DataEyeRank>> getTxhUrl(@RequestParam("rankingDate") String day);

}