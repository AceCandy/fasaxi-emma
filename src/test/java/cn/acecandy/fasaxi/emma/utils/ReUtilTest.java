package cn.acecandy.fasaxi.emma.utils;

import cn.hutool.v7.core.lang.mutable.MutablePair;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReUtilTest {

    @Test
    void parseTvNameSeason_shouldParseArabicAndChineseSeasonSuffix() {
        assertSeason("我不是药神", 2023, ReUtil.parseTvNameSeason("我不是药神2023"));
        assertSeason("我不是药神", 6, ReUtil.parseTvNameSeason("我不是药神第六季"));
        assertSeason("我不是药神", 2, ReUtil.parseTvNameSeason("我不是药神第2季"));
        assertSeason("我不是药神", 1, ReUtil.parseTvNameSeason("我不是药神 第一季"));
    }

    @Test
    void parseTvNameSeason_whenSeasonSuffixMissing_shouldKeepTitleAndReturnNullSeason() {
        MutablePair<String, Integer> result = ReUtil.parseTvNameSeason("我不是药神");

        assertEquals("我不是药神", result.getLeft());
        assertNull(result.getRight());
    }

    private void assertSeason(String expectedName, Integer expectedSeason, MutablePair<String, Integer> actual) {
        assertEquals(expectedName, actual.getLeft());
        assertEquals(expectedSeason, actual.getRight());
    }
}
