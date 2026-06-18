package cn.acecandy.fasaxi.emma.utils;

import cn.acecandy.fasaxi.emma.config.EmbyConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmbyProxyUtilTest {

    @Test
    void getPtUrlOnHk_shouldUseConfiguredPtTransferBase() {
        EmbyConfig embyConfig = new EmbyConfig();
        embyConfig.setTransPt1("http://alist.example.com/p/pt");

        String result = EmbyProxyUtil.getPtUrlOnHk(
                "http://192.168.1.205:5244/d/pt/Emby/series/test.mkv",
                embyConfig);

        assertEquals("http://alist.example.com/p/pt/Emby/series/test.mkv", result);
    }

    @Test
    void getPtUrlOnHk_shouldDeriveBtTransferBaseForEmby1() {
        EmbyConfig embyConfig = new EmbyConfig();
        embyConfig.setTransPt1("http://alist.example.com/p/pt");

        String result = EmbyProxyUtil.getPtUrlOnHk(
                "http://192.168.1.205:5244/d/pt/Emby1/series/test.mkv",
                embyConfig);

        assertEquals("http://alist.example.com/p/bt/Emby1/series/test.mkv", result);
    }
}
