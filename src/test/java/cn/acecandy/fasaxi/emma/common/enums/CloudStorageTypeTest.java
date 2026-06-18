package cn.acecandy.fasaxi.emma.common.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CloudStorageTypeTest {

    @Test
    void prefix_shouldUseHostIndependentDownloadPath() {
        assertEquals("/d/new115", CloudStorageType.R_115.getPrefix());
        assertEquals("/d/123", CloudStorageType.R_123.getPrefix());
        assertEquals("/d/zong123", CloudStorageType.R_123_ZONG.getPrefix());
    }
}
