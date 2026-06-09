package cn.acecandy.fasaxi.emma.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HtmlUtilTest {

    @Test
    void normalizeString_shouldRemoveNoiseAndLowercase() {
        assertEquals("再生魔人你好", HtmlUtil.normalizeString("再生'魔人: 你好"));
        assertEquals("", HtmlUtil.normalizeString(null));
    }
}
