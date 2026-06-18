package cn.acecandy.fasaxi.emma.utils;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileCacheUtilTest {

    @Test
    void getCachePath_shouldUseOnlyFileNameForHashSegment() {
        Path first = FileCacheUtil.getCachePath("12345", "Movie", "/vol2/media/test.mkv");
        Path second = FileCacheUtil.getCachePath("12345", "Movie", "/another/path/test.mkv");

        assertEquals(first, second);
        assertTrue(first.toString().startsWith("Movie/12345/"));
    }

    @Test
    void getCachePath_shouldChangeWhenFileNameChanges() {
        Path first = FileCacheUtil.getCachePath("12345", "Movie", "/vol2/media/test-1.mkv");
        Path second = FileCacheUtil.getCachePath("12345", "Movie", "/vol2/media/test-2.mkv");

        assertNotEquals(first, second);
    }
}
