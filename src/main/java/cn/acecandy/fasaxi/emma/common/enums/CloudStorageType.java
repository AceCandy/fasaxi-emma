package cn.acecandy.fasaxi.emma.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 云盘类型
 *
 * @author tangningzhu
 * @since 2025/8/19
 */
@AllArgsConstructor
@Getter
public enum CloudStorageType {
    /**
     * 115和123
     */
    R_115("115"),
    R_123("123");

    private final String value;
}