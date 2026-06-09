package cn.acecandy.fasaxi.emma.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PathUtilTest {

    @Test
    void replaceAfterUrlPath_whenPatternExists_shouldKeepSuffixAndReplaceBase() {
        String result = PathUtil.replaceAfterUrlPath(
                "https://alist.acecandy.cn:880/d/pt/emby1/国产剧/大明王朝/S01/E03.mkv",
                "/d/pt",
                "https://168.1.1.1:990/pt");

        assertEquals("https://168.1.1.1:990/pt/emby1/国产剧/大明王朝/S01/E03.mkv", result);
    }

    @Test
    void replaceAfterUrlPath_whenPatternMissing_shouldReturnOriginalUrl() {
        String url = "https://alist.acecandy.cn:880/d/other/E03.mkv";

        assertEquals(url, PathUtil.replaceAfterUrlPath(url, "/d/pt", "https://168.1.1.1:990/pt"));
    }
}
