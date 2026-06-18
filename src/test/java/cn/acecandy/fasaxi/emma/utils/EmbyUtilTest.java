package cn.acecandy.fasaxi.emma.utils;

import cn.acecandy.fasaxi.emma.config.EmbyConfig;
import cn.acecandy.fasaxi.emma.config.OpConfig;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmbyUtilTest {

    @Test
    void replacePath2Alist_shouldUseConfiguredPublicAlistPrefix() {
        EmbyUtil embyUtil = buildEmbyUtil("https://alist.example.com", "http://inner.example.local:5244",
                "https://download.example.com/d");

        String result = embyUtil.replacePath2Alist("https://alist.example.com/d/movie/test.mkv");

        assertEquals("/movie/test.mkv", result);
    }

    @Test
    void replacePath2Alist_shouldUseConfiguredInnerAlistPrefix() {
        EmbyUtil embyUtil = buildEmbyUtil("https://alist.example.com", "http://inner.example.local:5244",
                "https://download.example.com/d");

        String result = embyUtil.replacePath2Alist("http://inner.example.local:5244/d/series/test.mkv");

        assertEquals("/series/test.mkv", result);
    }

    @Test
    void replacePath2Alist_shouldUseConfiguredOpenlistDownloadPrefix() {
        EmbyUtil embyUtil = buildEmbyUtil("https://alist.example.com", "http://inner.example.local:5244",
                "https://download.example.com/d");

        String result = embyUtil.replacePath2Alist("https://download.example.com/d/new115/library/test.mkv");

        assertEquals("/new115/library/test.mkv", result);
    }

    private EmbyUtil buildEmbyUtil(String alistPublic, String alistInner, String downloadHost) {
        EmbyUtil embyUtil = new EmbyUtil();
        EmbyConfig embyConfig = new EmbyConfig();
        embyConfig.setAlistPublic(alistPublic);
        embyConfig.setAlistInner(alistInner);
        OpConfig opConfig = new OpConfig();
        opConfig.setDHost(downloadHost);
        ReflectionTestUtils.setField(embyUtil, "embyConfig", embyConfig);
        ReflectionTestUtils.setField(embyUtil, "opConfig", opConfig);
        return embyUtil;
    }
}
