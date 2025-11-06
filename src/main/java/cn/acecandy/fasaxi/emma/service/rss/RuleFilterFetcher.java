package cn.acecandy.fasaxi.emma.service.rss;

import cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType;
import cn.acecandy.fasaxi.emma.common.ex.BaseException;
import cn.acecandy.fasaxi.emma.dao.toolkit.entity.MediaMetadata;
import cn.acecandy.fasaxi.emma.sao.entity.MatchedItem;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.collection.set.SetUtil;
import cn.hutool.v7.core.date.DateField;
import cn.hutool.v7.core.date.DateTime;
import cn.hutool.v7.core.date.DateUtil;
import cn.hutool.v7.core.math.NumberUtil;
import cn.hutool.v7.core.reflect.FieldUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.json.JSONObject;
import cn.hutool.v7.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 规则过滤器 提取器
 *
 * @author AceCandy
 * @since 2025/10/31
 */
@Slf4j
@Service
public class RuleFilterFetcher {

    /**
     * 支持的列表字段 演员、导演、类型、国家/地区、工作室、标签、关键字
     */
    private final static Set<String> FIELDS_LIST = Set.of(
            "actors", "directors", "genres", "countries", "studios", "tags", "keywords");

    /**
     * 支持的日期字段 上映于、入库于
     */
    private final static Set<String> FIELDS_DATE = Set.of("release_date", "date_added");

    /**
     * 支持的分级字段 家长分级
     */
    private final static Set<String> FIELDS_RATING = Set.of("unified_rating");

    /**
     * 支持的连载剧集状态字段 追剧中
     */
    private final static Set<String> FIELDS_IN_PROGRESS = Set.of("is_in_progress");

    /**
     * 支持的标题字段
     */
    private final static Set<String> FIELDS_TITLE = Set.of("title");

    /**
     * 支持的数字字段 年份、评分
     */
    private final static Set<String> FIELDS_NUMBER = Set.of("release_year", "rating");


    private Set<String> airingIds;

    public RuleFilterFetcher(Set<String> airingIds) {
        this.airingIds = airingIds != null ? airingIds : new HashSet<>();
    }

    public Set<MatchedItem> exec(List<MediaMetadata> itemData, JSONObject definition) {
        List<MatchedItem.FilterRule> rules = definition.getBeanList("rules", MatchedItem.FilterRule.class);
        if (CollUtil.isEmpty(rules)) {
            return SetUtil.of();
        }
        String logic = definition.getStr("logic", "AND");

        List<String> itemTypeFromDb = JSONUtil.toList(definition.getStr("item_type"), String.class);
        EmbyMediaType itemType = EmbyMediaType.fromEmby(CollUtil.getFirst(itemTypeFromDb));

        return itemData.parallelStream()
                .filter(item -> checkRule(item, rules, logic))
                .map(item -> new MatchedItem(
                        Integer.parseInt(item.getTmdbId()), item.getTitle(), itemType, null))
                .collect(Collectors.toSet());
    }

    /**
     * 检查规则
     *
     * @param item  项目
     * @param rules 规则
     * @param logic 逻辑
     * @return boolean
     */
    private boolean checkRule(MediaMetadata item,
                              List<MatchedItem.FilterRule> rules, String logic) {
        List<Boolean> flagList = rules.parallelStream().map(rule -> {
            String field = rule.field();
            String fieldCamel = StrUtil.toCamelCase(field);
            String operator = rule.operator();
            String value = rule.value();

            return switch (field) {
                case String f when FIELDS_LIST.contains(f) -> handleListField(item, fieldCamel, operator, value);
                case String f when FIELDS_DATE.contains(f) -> handleDateField(item, fieldCamel, operator, value);

                case String f when FIELDS_RATING.contains(f) -> handleRatingField(item, fieldCamel, operator, value);
                case String f when FIELDS_IN_PROGRESS.contains(f) ->
                        handleInProgressField(item, fieldCamel, operator, value);
                case String f when FIELDS_TITLE.contains(f) -> handleTitleField(item, fieldCamel, operator, value);
                case String f when FIELDS_NUMBER.contains(f) -> handleGeneralField(item, fieldCamel, operator, value);
                default -> throw new BaseException("不支持的字段：" + field);
            };
        }).toList();

        return StrUtil.equalsIgnoreCase("AND", logic) ?
                flagList.stream().allMatch(Boolean::booleanValue) : flagList.stream().anyMatch(Boolean::booleanValue);
    }

    private boolean handleListField(MediaMetadata item, String field, String op, String value) {
        String itemValueStr = StrUtil.toStringOrNull(FieldUtil.getFieldValue(item, field + "_json"));
        if (StrUtil.isBlank(itemValueStr)) {
            return true;
        }
        List<String> itemList = JSONUtil.toList(itemValueStr, String.class);

        // 处理is_primary操作符
        if ("is_primary".equals(op)) {
            if ("actors".equals(field)) {
                itemList = CollUtil.sub(itemList, 0, 3);
            } else {
                itemList = CollUtil.sub(itemList, 0, 1);
            }
        }

        // 处理演员和导演特殊情况（对象列表）
        if (StrUtil.equalsAny(field, "actors", "directors")) {
            return handlePersonListField(itemList, op, value);
        }

        // 处理其他普通列表字段（字符串列表）
        return handleStringListField(itemList, op, value);
    }

    private boolean handlePersonListField(List<String> personList, String op, String value) {
        if (CollUtil.isEmpty(personList)) {
            return false;
        }
        Set<String> itemPersonIds = personList.stream()
                .map(person -> JSONUtil.toBean(person, MatchedItem.FieldPerson.class))
                .map(MatchedItem.FieldPerson::tmdb_id).filter(StrUtil::isNotBlank).collect(Collectors.toSet());

        List<MatchedItem.ValuePerson> rulePersons = JSONUtil.toList(value, MatchedItem.ValuePerson.class);
        Set<String> rulePersonIds = rulePersons.stream().map(MatchedItem.ValuePerson::id)
                .filter(StrUtil::isNotBlank).collect(Collectors.toSet());
        if (CollUtil.isEmpty(rulePersonIds)) {
            return false;
        }

        return switch (op) {
            case "is_one_of", "contains", "is_primary" -> !Collections.disjoint(rulePersonIds, itemPersonIds);
            case "is_none_of" -> Collections.disjoint(rulePersonIds, itemPersonIds);
            default -> false;
        };
    }

    private boolean handleStringListField(List<String> stringValues, String op, String value) {
        return switch (op) {
            case "is_primary" -> CollUtil.isNotEmpty(stringValues)
                    && StrUtil.equals(CollUtil.getFirst(stringValues), value);
            case "is_one_of" -> {
                List<String> valueList = JSONUtil.toList(value, String.class);
                yield valueList.stream().anyMatch(stringValues::contains);
            }
            case "is_none_of" -> {
                List<String> valueList = JSONUtil.toList(value, String.class);
                yield valueList.stream().noneMatch(stringValues::contains);
            }
            case "contains" -> stringValues.contains(value);
            default -> false;
        };
    }

    private boolean handleDateField(MediaMetadata item, String field, String op, String value) {
        String itemValueStr = StrUtil.toStringOrNull(FieldUtil.getFieldValue(item, field));
        if (StrUtil.isBlank(itemValueStr) || !NumberUtil.isNumber(value)) {
            return true;
        }
        int days = Integer.parseInt(value);
        // 前N天
        DateTime cutoffDate = DateUtil.now().offset(DateField.DAY_OF_MONTH, -days);

        DateTime itemDate;
        try {
            itemDate = DateUtil.parse(itemValueStr);
        } catch (Exception e) {
            // yyyy-MM-dd
            if (StrUtil.length(itemValueStr) != 10) {
                return false;
            }
            itemDate = DateUtil.parse(itemValueStr, DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return switch (op) {
            case "in_last_days" -> !itemDate.isBefore(cutoffDate);
            case "not_in_last_days" -> itemDate.isBefore(cutoffDate);
            default -> false;
        };
    }

    private boolean handleRatingField(MediaMetadata item, String field,
                                      String op, String value) {
        String itemValueStr = StrUtil.toStringOrNull(FieldUtil.getFieldValue(item, field));
        if (StrUtil.isBlank(itemValueStr)) {
            return true;
        }

        return switch (op) {
            case "is_one_of" -> {
                List<String> valueList = JSONUtil.toList(value, String.class);
                yield valueList.contains(itemValueStr);
            }
            case "is_none_of" -> {
                List<String> valueList = JSONUtil.toList(value, String.class);
                yield !valueList.contains(itemValueStr);
            }
            case "eq" -> StrUtil.equals(itemValueStr, value);
            default -> false;
        };
    }

    private boolean handleInProgressField(MediaMetadata item, String field,
                                          String op, String value) {
        String itemType = item.getItemType();
        if (!StrUtil.equals("Series", itemType)) {
            return false;
        }

        String tmdbId = item.getTmdbId();
        if (StrUtil.isBlank(tmdbId)) {
            return false;
        }
        boolean isItemAiring = airingIds.contains(tmdbId);
        boolean targetValue = Boolean.parseBoolean(value);

        return switch (op) {
            case "is" -> isItemAiring == targetValue;
            case "is_not" -> isItemAiring != targetValue;
            default -> false;
        };
    }

    private boolean handleTitleField(MediaMetadata item, String field,
                                     String op, String value) {
        String itemValueStr = StrUtil.toStringOrNull(FieldUtil.getFieldValue(item, field));
        if (StrUtil.isBlank(itemValueStr)) {
            return true;
        }
        String searchValue = value.toLowerCase();

        return switch (op) {
            case "contains" -> StrUtil.contains(itemValueStr, searchValue);
            case "does_not_contain" -> !StrUtil.contains(itemValueStr, searchValue);
            case "starts_with" -> StrUtil.startWith(itemValueStr, searchValue);
            case "ends_with" -> StrUtil.endWith(itemValueStr, searchValue);
            default -> false;
        };
    }

    private boolean handleGeneralField(MediaMetadata item, String field,
                                       String op, String value) {
        String itemValueStr = StrUtil.toStringOrNull(FieldUtil.getFieldValue(item, field));
        if (StrUtil.isBlank(itemValueStr)) {
            return true;
        }
        return switch (op) {
            case "gte" -> Double.parseDouble(itemValueStr) >= Double.parseDouble(value);
            case "lte" -> Double.parseDouble(itemValueStr) <= Double.parseDouble(value);
            case "eq" -> StrUtil.equals(itemValueStr, value);
            default -> false;
        };
    }

}