package cn.acecandy.fasaxi.emma.common.enums;

import cn.acecandy.fasaxi.emma.common.ex.BaseException;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.lang.mutable.MutableTriple;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.core.text.split.SplitUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * strm路径前缀
 *
 * @author tangningzhu
 * @since 2025/8/19
 */
@AllArgsConstructor
@Getter
public enum StrmPathPrefix {
    /**
     * 115和123
     */
    PRE_MICU("/d/pt/", "micu"),
    PRE_123("/d/123/", "123"),
    PRE_ZONG123("/d/zong123/", "123"),
    PRE_NEW115("/d/new115/", "115"),
    PRE_115("/d/115/", "115"),
    PRE_LOCAL("vvvvvvvvv", "local"),
    ;

    private final String value;
    private final String type;

    public static StrmPathPrefix of(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        for (StrmPathPrefix prefix : values()) {
            if (StrUtil.equals(prefix.value, value)) {
                return prefix;
            }
        }
        return null;
    }

    public static StrmPathPrefix findByFullPath(String fullPath) {
        if (StrUtil.isBlank(fullPath)) {
            return null;
        }
        for (StrmPathPrefix prefix : values()) {
            if (StrUtil.contains(fullPath, prefix.value)) {
                return prefix;
            }
        }
        return null;
    }

    /**
     * 分割为域名、前缀、清洁路径
     *
     * @param fullPath 完整路径
     * @return {@link MutableTriple }<{@link String }, {@link StrmPathPrefix }, {@link String }>
     */
    public static MutableTriple<String, StrmPathPrefix, String> split(String fullPath) {
        if (StrUtil.isBlank(fullPath)) {
            throw new BaseException("fullPath不能为空");
        }
        for (StrmPathPrefix prefix : values()) {
            if (StrUtil.contains(fullPath, prefix.value)) {
                List<String> split = SplitUtil.splitTrim(fullPath, prefix.value);
                return MutableTriple.of(CollUtil.getFirst(split), prefix, CollUtil.getLast(split));
            }
        }
        return MutableTriple.of("", PRE_LOCAL, fullPath);
    }
}