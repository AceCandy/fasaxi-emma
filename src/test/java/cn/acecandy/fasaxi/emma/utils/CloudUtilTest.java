package cn.acecandy.fasaxi.emma.utils;

import cn.acecandy.fasaxi.emma.common.enums.CloudStorageType;
import cn.acecandy.fasaxi.emma.config.OpConfig;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CloudUtilTest {

    @Test
    void buildOpenlistMediaPath_shouldPreferDownloadHostWhenConfigured() {
        CloudUtil cloudUtil = buildCloudUtil("https://api.example.com", "https://download.example.com/d");

        String result = cloudUtil.buildOpenlistMediaPath(CloudStorageType.R_115, "/library/movie/test.mkv");

        assertEquals("https://download.example.com/d/new115/library/movie/test.mkv", result);
    }

    @Test
    void buildOpenlistMediaPath_shouldFallbackToHostWhenDownloadHostMissing() {
        CloudUtil cloudUtil = buildCloudUtil("https://api.example.com", null);

        String result = cloudUtil.buildOpenlistMediaPath(CloudStorageType.R_123, "/library/movie/test.mkv");

        assertEquals("https://api.example.com/d/123/library/movie/test.mkv", result);
    }

    @Test
    void normalizeOpenlistMediaPath_shouldRewriteLegacyHostToConfiguredDownloadHost() {
        CloudUtil cloudUtil = buildCloudUtil("https://api.example.com", "https://download.example.com/d");

        String result = cloudUtil.normalizeOpenlistMediaPath(
                CloudStorageType.R_115,
                "http://192.168.1.249:5244/d/new115/library/movie/test.mkv");

        assertEquals("https://download.example.com/d/new115/library/movie/test.mkv", result);
    }

    @Test
    void normalizeOpenlistMediaPath_shouldKeepUnrelatedHttpUrlUntouched() {
        CloudUtil cloudUtil = buildCloudUtil("https://api.example.com", "https://download.example.com/d");

        String result = cloudUtil.normalizeOpenlistMediaPath(
                CloudStorageType.R_115,
                "https://cdn.example.com/video.mkv?t=123456");

        assertEquals("https://cdn.example.com/video.mkv?t=123456", result);
    }

    private CloudUtil buildCloudUtil(String host, String dHost) {
        CloudUtil cloudUtil = new CloudUtil();
        OpConfig opConfig = new OpConfig();
        opConfig.setHost(host);
        opConfig.setDHost(dHost);
        ReflectionTestUtils.setField(cloudUtil, "opConfig", opConfig);
        return cloudUtil;
    }
}
