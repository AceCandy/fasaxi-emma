package cn.acecandy.fasaxi.emma.dao.embyboss.service;

import cn.acecandy.fasaxi.emma.config.OpConfig;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VideoPathRelationDaoTest {

    @Test
    void buildOpenlistPath_shouldUseConfiguredHostWithoutDuplicateSlash() {
        VideoPathRelationDao dao = new VideoPathRelationDao();
        OpConfig opConfig = new OpConfig();
        opConfig.setHost("https://openlist.example.com/");
        ReflectionTestUtils.setField(dao, "opConfig", opConfig);

        String result = dao.buildOpenlistPath("/d/new115/", "movie/test.mkv");

        assertEquals("https://openlist.example.com/d/new115/movie/test.mkv", result);
    }

    @Test
    void buildOpenlistPath_shouldReturnEmptyWhenHostMissing() {
        VideoPathRelationDao dao = new VideoPathRelationDao();
        ReflectionTestUtils.setField(dao, "opConfig", new OpConfig());

        String result = dao.buildOpenlistPath("/d/new115/", "movie/test.mkv");

        assertEquals("", result);
    }
}
