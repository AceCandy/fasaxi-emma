package cn.acecandy.fasaxi.emma.utils;

import cn.acecandy.fasaxi.emma.common.enums.CloudStorageType;
import cn.acecandy.fasaxi.emma.sao.dto.Rile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CloudPathCacheUtilTest {

    @AfterEach
    void tearDown() {
        CloudPathCacheUtil.clear();
    }

    @Test
    void find_shouldReturnCachedPrefixMatch() {
        Rile root = new Rile();
        root.setFileId(1L);
        Rile child = new Rile();
        child.setFileId(2L);
        CloudPathCacheUtil.put(CloudStorageType.R_115, "/library", root);
        CloudPathCacheUtil.put(CloudStorageType.R_115, "/library/movie", child);

        Map.Entry<String, Rile> result = CloudPathCacheUtil.find(CloudStorageType.R_115,
                "/library/movie/test.mkv");

        assertNotNull(result);
        assertEquals("R_115:/library", result.getKey());
        assertEquals(1L, result.getValue().getFileId());
    }

    @Test
    void remove_shouldDeleteSpecificCacheEntry() {
        CloudPathCacheUtil.put(CloudStorageType.R_123, "/library", new Rile());

        CloudPathCacheUtil.remove(CloudStorageType.R_123, "/library");

        assertNull(CloudPathCacheUtil.get(CloudStorageType.R_123, "/library"));
    }
}
