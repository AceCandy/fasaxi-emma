package cn.acecandy.fasaxi.emma.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dromara.hutool.core.text.StrUtil;

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
    R_115("115", "http://192.168.1.249:5244/d/new115"),
    R_123("123", "http://192.168.1.249:5244/d/123"),
    R_123_ZONG("123_zong", "http://192.168.1.249:5244/d/zong123"),
    /**
     * 本地
     */
    L_NC2O("nc2o", ""),
    ;

    private final String value;
    private final String prefix;

    public static CloudStorageType of(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        for (CloudStorageType cloudStorageType : values()) {
            if (StrUtil.equals(cloudStorageType.value, value)) {
                return cloudStorageType;
            }
        }
        return null;
    }
}