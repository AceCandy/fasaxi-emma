package cn.acecandy.fasaxi.emma.utils;

import cn.acecandy.fasaxi.emma.sao.out.EmbyItem;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.core.math.NumberUtil;
import cn.hutool.v7.core.regex.RegexPool;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.core.text.split.SplitUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static cn.acecandy.fasaxi.emma.utils.ReUtil.REGEX_SPILT_TITLE;


/**
 * 排序工具类
 *
 * @author tangningzhu
 * @since 2023/12/20
 */
@Slf4j
public final class SortUtil {

    private static final String[] REMOVE_SYMBOL = {",", ".", "，", "。", "；",
            ";", "'", "\"", "“", "”", "‘", "’", "：", ":", "【", "】"};

    private SortUtil() {
    }

    /**
     * 搜索排序
     *
     * @param items  项目
     * @param search 搜索
     * @return {@link List }<{@link EmbyItem }>
     */
    public static List<EmbyItem> searchSortItem(List<EmbyItem> items, String search) {
        // 预处理
        final String searchLower = search.toLowerCase();
        items.forEach(item -> item.setProductionYear(NumberUtil.nullToZero(item.getProductionYear())));

        // 使用EnumMap分组
        EnumMap<SearchMatchGroup, List<EmbyItem>> groups = new EnumMap<>(SearchMatchGroup.class);
        Arrays.stream(SearchMatchGroup.values()).forEach(g -> groups.put(g, ListUtil.of()));

        // 单次遍历分组
        items.forEach(item -> {
            String nameLower = item.getName().toLowerCase();
            String noSymbolName = StrUtil.removeAll(nameLower, REMOVE_SYMBOL);
            List<String> splitName = SplitUtil.splitByRegex(nameLower, REGEX_SPILT_TITLE, 0, true, true);

            groups.get(SearchMatchGroup.classify(item, searchLower, noSymbolName, splitName)).add(item);
        });

        // 构建统一比较器 年份降序->数字降序->关键字位置
        Comparator<EmbyItem> comparator = Comparator
                .comparingInt((EmbyItem e) -> -e.getProductionYear())
                .thenComparing(e -> -extractFirstNumber(e.getName()))
                .thenComparing(e -> -StrUtil.lastIndexOfIgnoreCase(e.getName(), search));

        // 分组排序（OTHERS保持原序）
        groups.forEach((group, list) -> {
            if (group == SearchMatchGroup.其它) {
                return;
            }
            CollUtil.sort(list, comparator);
        });

        // 合并结果流处理
        return groups.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> e.getKey().ordinal()))
                .flatMap(entry -> entry.getValue().stream())
                .peek(item -> {
                    if (item.getProductionYear() == 0) {
                        item.setProductionYear(null);
                    }
                })
                .collect(Collectors.toList());

        /*// 全匹配
        List<EmbyItem> exactMatch = ListUtil.of();
        // 全匹配(剔除符号)
        List<EmbyItem> noSymbolExactMatch = ListUtil.of();
        // 分割后全匹配(符号分割)
        List<EmbyItem> splitExactMatch = ListUtil.of();
        // 无书名号
        List<EmbyItem> containsNoBracket = ListUtil.of();
        // 包含有书名号
        List<EmbyItem> containsWithBracket = ListUtil.of();
        // 其他元素
        List<EmbyItem> others = ListUtil.of();

        String searchLower = search.toLowerCase();
        items.forEach(item -> item.setProductionYear(ObjUtil.defaultIfNull(item.getProductionYear(), 0)));

        // 第一遍遍历：分组处理
        for (EmbyItem item : items) {
            String name = item.getName();
            String noSymbolName = StrUtil.removeAll(name, REMOVE_SYMBOL);
            List<String> splitName = SplitUtil.splitByRegex(
                    name, REGEX_SPILT_TITLE, 0, true, true);

            if (StrUtil.equalsIgnoreCase(name, search)) {
                exactMatch.add(item);
            } else if (StrUtil.equalsIgnoreCase(noSymbolName, search)) {
                noSymbolExactMatch.add(item);
            } else if (CollUtil.contains(splitName, search)) {
                splitExactMatch.add(item);
            } else if (StrUtil.containsIgnoreCase(name, search)) {
                if (hasBookBrackets(name)) {
                    containsWithBracket.add(item);
                } else {
                    containsNoBracket.add(item);
                }
            } else {
                others.add(item);
            }
        }

        // 定义包含组的排序规则（关键字位置 + 年份降序）
        Comparator<EmbyItem> containsComparator = Comparator
                .comparingInt((EmbyItem b) -> StrUtil.lastIndexOfIgnoreCase(b.getName(), search))
                .thenComparingInt(EmbyItem::getProductionYear).reversed()
                .thenComparing((item1, item2) -> {
                    Integer num1 = NumberUtil.parseInt(CollUtil.getFirst(
                            ReUtil.findAllGroup0(RegexPool.NUMBERS, item1.getName())), 0);
                    Integer num2 = NumberUtil.parseInt(CollUtil.getFirst(
                            ReUtil.findAllGroup0(RegexPool.NUMBERS, item2.getName())), 0);
                    return num2.compareTo(num1);
                });

        Comparator<EmbyItem> matchComparator = Comparator.comparingInt(EmbyItem::getProductionYear).reversed()
                .thenComparing((item1, item2) -> {
                    Integer num1 = NumberUtil.parseInt(CollUtil.getFirst(
                            ReUtil.findAllGroup0(RegexPool.NUMBERS, item1.getName())), 0);
                    Integer num2 = NumberUtil.parseInt(CollUtil.getFirst(
                            ReUtil.findAllGroup0(RegexPool.NUMBERS, item2.getName())), 0);
                    return num2.compareTo(num1);
                });

        // 分组排序
        exactMatch.sort(matchComparator);
        noSymbolExactMatch.sort(matchComparator);
        splitExactMatch.sort(matchComparator);
        containsNoBracket.sort(containsComparator);
        containsWithBracket.sort(containsComparator);

        // 合并结果（保持others原序）
        List<EmbyItem> result = new ArrayList<>();
        result.addAll(exactMatch);
        result.addAll(noSymbolExactMatch);
        result.addAll(splitExactMatch);
        result.addAll(containsNoBracket);
        result.addAll(containsWithBracket);
        result.addAll(others);

        result.replaceAll(item -> {
            if (0 == item.getProductionYear()) {
                item.setProductionYear(null);
            }
            return item;
        });

        return result;*/
    }

    /**
     * 提取第一个数字
     *
     * @param name 名称
     * @return int
     */
    private static int extractFirstNumber(String name) {
        return Optional.ofNullable(ReUtil.findAllGroup0(RegexPool.NUMBERS, name))
                .map(CollUtil::getFirst).map(NumberUtil::parseInt)
                .orElse(0);
    }

    /**
     * 有书名号
     *
     * @param name 名称
     * @return boolean
     */
    private static boolean hasBookBrackets(String name) {
        return StrUtil.containsAny(name, "《", "》");
    }

    private enum SearchMatchGroup {
        // 搜索匹配分组
        全匹配,
        剔除符号全匹配,
        分割后全匹配,
        普通包含,
        普通包含_带书名号,
        其它;

        /**
         * 分组判断逻辑
         */
        public static SearchMatchGroup classify(EmbyItem item, String searchLower,
                                                String noSymbolName, List<String> splitName) {
            String nameLower = item.getName().toLowerCase();

            if (StrUtil.equals(nameLower, searchLower)) return 全匹配;
            if (StrUtil.equals(noSymbolName, searchLower)) return 剔除符号全匹配;
            if (CollUtil.contains(splitName, searchLower)) return 分割后全匹配;
            if (nameLower.contains(searchLower)) {
                return hasBookBrackets(nameLower) ? 普通包含_带书名号 : 普通包含;
            }
            return 其它;
        }
    }
}
