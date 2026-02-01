package cn.acecandy.fasaxi.emma.service.rss;

import cn.acecandy.fasaxi.emma.dao.embyboss.entity.JavRank;
import cn.acecandy.fasaxi.emma.dao.embyboss.entity.SdJccRank;
import cn.acecandy.fasaxi.emma.dao.embyboss.service.JavRankDao;
import cn.acecandy.fasaxi.emma.sao.entity.MatchedItem;
import cn.acecandy.fasaxi.emma.sao.out.EmbyItem;
import cn.acecandy.fasaxi.emma.sao.proxy.EmbyProxy;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.core.collection.set.SetUtil;
import cn.hutool.v7.core.map.MapUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.core.text.split.SplitUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * jav rank 处理器
 *
 * @author tangningzhu
 * @since 2025/10/29
 */
@Slf4j
@Service
public class JavRankFetcher {


    @Resource
    private EmbyProxy embyProxy;

    @Resource
    private JavRankDao javRankDao;


    /**
     * 库ID
     */
    private static final Integer PARENT_ID = 294462;

    /**
     * 固定Top30数量
     */
    private static final int TOP_NUM = 50;

    /**
     * 核心处理
     * <p>
     * JavRank=影片榜:maxDate
     * JavRank=JavDB有码-TOP250:202601
     * JavRank=JavLibray-TOP500:202601
     * JavRank=JavDB有码-月榜:maxDate
     *
     * @param url 路径
     * @return {@link List }<{@link MatchedItem }>
     */
    public Set<String> exec(String url) {
        String keyword = StrUtil.removePrefix(url, "JavRank=");
        List<String> keywords = SplitUtil.split(keyword, ":");
        String rankType = CollUtil.getFirst(keywords);
        String rankDate = CollUtil.getLast(keywords);

        JavRank rank = null;
        if (StrUtil.equals(rankDate, "maxDate")) {
            rank = javRankDao.findByTypeOnMaxDate(rankType);
        } else {
            rank = javRankDao.findByTypeAndDate(rankType, rankDate);
        }
        if (null == rank) {
            return SetUtil.ofLinked();
        }
        List<String> rankList = rank.getSerList();
        Set<String> matchedItems = SetUtil.ofLinked();
        rankList.forEach(t -> {
            List<EmbyItem> items = embyProxy.searchItemsByCollections(t, "Movie", PARENT_ID);
            if (CollUtil.isEmpty(items)) {
                return;
            }

            EmbyItem bestMatch = CollUtil.getFirst(items);
            String bestMatchName = CollUtil.getFirst(SplitUtil.splitTrim(bestMatch.getName(), " "));
            if (!StrUtil.equals(bestMatchName, t)) {
                return;
            }
            matchedItems.add(bestMatch.getItemId());
        });
        return matchedItems;
    }

    /**
     * 子方法1：解析并校验输入参数，返回解析后的参数映射
     */
    private Map<String, Object> parseAndCheckParam(String param) {
        if (param == null || !param.contains(":")) {
            throw new IllegalArgumentException("参数格式错误，必须为：type:time_stage:time_value_prefix（如4:month:2025）");
        }
        String[] arr = param.split(":");
        if (arr.length != 3) {
            throw new IllegalArgumentException("参数必须为3段，当前为" + arr.length + "段：" + param);
        }
        // 解析type并校验为数字
        Integer type;
        try {
            type = Integer.parseInt(arr[0].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("type必须为数字，当前值：" + arr[0]);
        }
        String timeStage = arr[1].trim();
        String timeValuePrefix = arr[2].trim();
        // 封装返回
        Map<String, Object> result = new HashMap<>(3);
        result.put("type", type);
        result.put("timeStage", timeStage);
        result.put("timeValuePrefix", timeValuePrefix);
        return result;
    }

    /**
     * 子方法2：内存统计→遍历所有原始数据，解析JSON并统计短剧ID出现频次
     *
     * @param ranks 原始榜单数据
     * @return 频次映射：key=短剧ID，value=出现次数
     */
    private Map<Long, Integer> statisticsDramaIdFrequency(List<SdJccRank> ranks) {
        Map<Long, Integer> frequencyMap = MapUtil.newHashMap();
        for (SdJccRank rank : ranks) {
            List<Long> rankList = rank.getRankList();
            if (CollUtil.isEmpty(rankList)) {
                continue;
            }
            // 遍历ID，统计频次（出现一次则计数+1）
            for (Long r : rankList) {
                if (null == r) {
                    continue;
                }
                frequencyMap.put(r, frequencyMap.getOrDefault(r, 0) + 1);
            }
        }
        return frequencyMap;
    }

    /**
     * 子方法3：筛选TopN→按频次降序排序，超30截前30，不足保留全部
     *
     * @param frequencyMap 频次映射
     * @return 按频次排序的短剧ID列表
     */
    private List<Long> filterTopDramaIds(Map<Long, Integer> frequencyMap) {
        // 按频次降序排序，频次相同则按ID升序（保证排序一致性）
        return frequencyMap.entrySet().stream()
                .sorted((e1, e2) -> {
                    int countCompare = e2.getValue().compareTo(e1.getValue());
                    return countCompare != 0 ? countCompare : e1.getKey().compareTo(e2.getKey());
                })
                .map(Map.Entry::getKey)
                // 超30截前30，不足保留全部
                .limit(Math.min(frequencyMap.size(), TOP_NUM))
                .collect(Collectors.toList());
    }

    /**
     * 子方法4：补全至30→从最后时间维度（最后一条数据）补充未上榜的短剧ID
     *
     * @param currentList 当前TopN列表（不足30）
     * @param rawDataList 原始数据列表（最后一条为最后一月/最后一天）
     * @return 补全后的Top30列表
     */
    private List<Long> supplementTo30(List<Long> currentList, List<SdJccRank> rawDataList) {
        List<Long> resultList = ListUtil.of(currentList);
        // 已上榜ID，快速去重（O(1)判断）
        Set<Long> existIds = SetUtil.of(currentList);

        // 获取最后一个时间维度的榜单数据（列表最后一条）
        SdJccRank lastRank = CollUtil.getLast(rawDataList);
        List<Long> lastRankList = lastRank.getRankList();
        if (CollUtil.isEmpty(lastRankList)) {
            return resultList;
        }

        // 按原榜单顺序，补充未上榜的ID，直到满30
        for (Long dramaId : lastRankList) {
            if (!existIds.contains(dramaId) && CollUtil.size(resultList) < TOP_NUM) {
                resultList.add(dramaId);
                // 标记为已补充，避免重复
                existIds.add(dramaId);
            }
            if (CollUtil.size(resultList) == TOP_NUM) {
                break; // 补满30，立即停止
            }
        }

        return resultList;
    }
}