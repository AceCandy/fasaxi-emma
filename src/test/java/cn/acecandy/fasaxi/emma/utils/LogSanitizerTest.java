package cn.acecandy.fasaxi.emma.utils;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogSanitizerTest {

    @Test
    void sanitizeQueryString_shouldMaskSensitiveValuesOnly() {
        String sanitized = LogSanitizer.sanitizeQueryString("api_key=abcdef123456&Name=demo&X-Emby-Token=xyz123");

        assertTrue(sanitized.contains("api_key=abc***56"));
        assertTrue(sanitized.contains("Name=demo"));
        assertTrue(sanitized.contains("X-Emby-Token=***"));
    }

    @Test
    void sanitizeHeadersForLog_shouldKeepOnlySafeHeaders() {
        Map<String, String> sanitized = LogSanitizer.sanitizeHeadersForLog(Map.of(
                "User-Agent", "JUnit",
                "Authorization", "Bearer secret",
                "X-Forwarded-For", "127.0.0.1"
        ));

        assertEquals("JUnit", sanitized.get("User-Agent"));
        assertEquals("127.0.0.1", sanitized.get("X-Forwarded-For"));
        assertFalse(sanitized.containsKey("Authorization"));
    }

    @Test
    void sanitizeTextForLog_shouldMaskSensitiveHeadersQueryAndJsonFields() {
        assertEquals("Authorization: Bearer abc***56",
                LogSanitizer.sanitizeTextForLog("Authorization: Bearer abcdef123456"));
        assertEquals("---> GET https://example.com/api?api_key=abc***56&name=demo HTTP/1.1",
                LogSanitizer.sanitizeTextForLog(
                        "---> GET https://example.com/api?api_key=abcdef123456&name=demo HTTP/1.1"));
        assertEquals("{\"access_token\":\"abc***56\",\"name\":\"demo\",\"refresh_token\":\"ref***45\"}",
                LogSanitizer.sanitizeTextForLog(
                        "{\"access_token\":\"abcdef123456\",\"name\":\"demo\",\"refresh_token\":\"refresh12345\"}"));
    }
}
