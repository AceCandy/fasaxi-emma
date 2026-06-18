package cn.acecandy.fasaxi.emma.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PinYinUtilTest {

    @Test
    void findAllChar_shouldRequireEveryCharToExist() {
        assertFalse(PinYinUtil.findAllChar("奇奇怪怪的曲", "曲奇1"));
        assertTrue(PinYinUtil.findAllChar("白色板子", "白板"));
    }

    @Test
    void findTwoChar_shouldReturnTrueWhenAtLeastTwoCharsExist() {
        assertTrue(PinYinUtil.findTwoChar("白色板子", "白板"));
        assertFalse(PinYinUtil.findTwoChar("白色板子", "白"));
        assertTrue(PinYinUtil.findTwoChar("奇奇怪怪的曲", "曲奇1"));
    }
}
