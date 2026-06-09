package cn.acecandy.fasaxi.emma.config;

import cn.hutool.v7.core.collection.ListUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbyConfigTest {

    @Test
    void normalizeStrmPath_shouldUseLongestMatchedPrefix() {
        EmbyConfig config = new EmbyConfig();
        config.setStrmPaths(new String[]{
                "/mnt/all/::http://alist.example.com/d/all/",
                "/mnt/all/cd2/new115/::http://alist.example.com/d/new115/"
        });

        String normalized = config.normalizeStrmPath("/mnt/all/cd2/new115/movie/test.mkv");

        assertEquals("http://alist.example.com/d/new115/movie/test.mkv", normalized);
    }

    @Test
    void getPathMap_shouldTrimEntriesIgnoreInvalidAndKeepLastDuplicate() {
        EmbyConfig config = new EmbyConfig();
        config.setLocalPaths(ListUtil.of(
                " /mnt/all/ :: http://alist.example.com/d/all/ ",
                "invalid-entry",
                "/mnt/all/::http://alist.example.com/d/latest/"
        ));

        assertEquals("http://alist.example.com/d/latest/", config.getLocalPathMap().get("/mnt/all/"));
        assertFalse(config.getLocalPathMap().containsKey("invalid-entry"));
        assertTrue(config.isLocalPath("/mnt/all/movie/test.mkv"));
    }
}
