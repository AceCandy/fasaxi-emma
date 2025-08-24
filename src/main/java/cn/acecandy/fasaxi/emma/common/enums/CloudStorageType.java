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
    R_115("115"),
    R_123("123"),
    R_123_ZONG("123_zong"),
    /**
     * 本地
     */
    L_NC2O("nc2o"),
    ;

    private final String value;

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