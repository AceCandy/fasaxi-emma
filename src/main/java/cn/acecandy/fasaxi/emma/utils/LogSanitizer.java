package cn.acecandy.fasaxi.emma.utils;

import cn.hutool.v7.core.map.MapUtil;
import cn.hutool.v7.core.text.StrUtil;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志脱敏工具
 *
 * @author Codex
 * @since 2026/06/10
 */
public final class LogSanitizer {

    private static final Set<String> SENSITIVE_QUERY_KEYS = Set.of(
            "api_key", "x-emby-token", "x-emby-authorization",
            "token", "authorization", "cookie", "access_token", "refresh_token");

    private static final Set<String> SENSITIVE_HEADER_NAMES = Set.of(
            "authorization", "proxy-authorization", "cookie", "set-cookie",
            "x-emby-authorization", "x-emby-token");

    private static final Set<String> SAFE_LOG_HEADER_NAMES = Set.of(
            "Accept", "Accept-Encoding", "Content-Type", "Range",
            "Referer", "User-Agent", "X-Forwarded-For", "X-Real-Ip");

    private static final Pattern HEADER_LINE_PATTERN = Pattern.compile("^([A-Za-z0-9-]+):(\\s*)(.*)$");
    private static final Pattern JSON_SENSITIVE_FIELD_PATTERN = Pattern.compile(
            "(\"(?:api_key|x-emby-token|x-emby-authorization|token|authorization|cookie|access_token|refresh_token)\"\\s*:\\s*\")([^\"]*)(\")",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern QUERY_SENSITIVE_FIELD_PATTERN = Pattern.compile(
            "((?:api_key|x-emby-token|x-emby-authorization|token|authorization|cookie|access_token|refresh_token)=)([^&\\s\",}]+)",
            Pattern.CASE_INSENSITIVE);

    private LogSanitizer() {
    }

    public static String sanitizeQueryString(String queryString) {
        if (StrUtil.isBlank(queryString)) {
            return "";
        }
        return StrUtil.join("&", Arrays.stream(queryString.split("&"))
                .filter(StrUtil::isNotBlank)
                .map(LogSanitizer::sanitizeQueryPair)
                .toList());
    }

    public static String sanitizeUri(String uri) {
        if (StrUtil.isBlank(uri)) {
            return uri;
        }
        int queryIndex = StrUtil.indexOf(uri, '?');
        if (queryIndex < 0) {
            return uri;
        }
        String path = StrUtil.subPre(uri, queryIndex);
        String queryString = StrUtil.subSuf(uri, queryIndex + 1);
        String sanitizedQuery = sanitizeQueryString(queryString);
        if (StrUtil.isBlank(sanitizedQuery)) {
            return path;
        }
        return path + "?" + sanitizedQuery;
    }

    public static Map<String, String> sanitizeHeadersForLog(Map<String, String> headers) {
        if (MapUtil.isEmpty(headers)) {
            return Map.of();
        }
        Map<String, String> sanitizedHeaders = new LinkedHashMap<>();
        headers.forEach((name, value) -> {
            if (!SAFE_LOG_HEADER_NAMES.contains(name)) {
                return;
            }
            sanitizedHeaders.put(name, value == null ? "" : value);
        });
        return sanitizedHeaders;
    }

    public static String sanitizeTextForLog(String text) {
        if (StrUtil.isBlank(text)) {
            return text;
        }
        String sanitized = sanitizeHeaderLine(text);
        sanitized = sanitizeJsonText(sanitized);
        return sanitizeQueryText(sanitized);
    }

    private static String sanitizeQueryPair(String pair) {
        int equalIndex = StrUtil.indexOf(pair, '=');
        if (equalIndex < 0) {
            return pair;
        }
        String key = StrUtil.subPre(pair, equalIndex);
        String value = StrUtil.subSuf(pair, equalIndex + 1);
        if (SENSITIVE_QUERY_KEYS.contains(StrUtil.lowerFirst(key).toLowerCase())) {
            return key + "=" + mask(value);
        }
        return pair;
    }

    private static String sanitizeHeaderLine(String text) {
        Matcher matcher = HEADER_LINE_PATTERN.matcher(text);
        if (!matcher.matches()) {
            return text;
        }
        String headerName = matcher.group(1);
        if (!SENSITIVE_HEADER_NAMES.contains(headerName.toLowerCase(Locale.ROOT))) {
            return text;
        }
        String spacing = matcher.group(2);
        String value = matcher.group(3);
        return headerName + ":" + spacing + maskHeaderValue(headerName, value);
    }

    private static String sanitizeJsonText(String text) {
        return replaceMaskedGroup(text, JSON_SENSITIVE_FIELD_PATTERN, 2);
    }

    private static String sanitizeQueryText(String text) {
        return replaceMaskedGroup(text, QUERY_SENSITIVE_FIELD_PATTERN, 2);
    }

    private static String replaceMaskedGroup(String text, Pattern pattern, int valueGroup) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String replacement = matcher.group();
            String value = matcher.group(valueGroup);
            int valueStart = matcher.start(valueGroup) - matcher.start();
            int valueEnd = matcher.end(valueGroup) - matcher.start();
            replacement = replacement.substring(0, valueStart) + mask(value) + replacement.substring(valueEnd);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String maskHeaderValue(String headerName, String value) {
        if (StrUtil.equalsAnyIgnoreCase(headerName, "Authorization", "Proxy-Authorization", "X-Emby-Authorization")) {
            String[] parts = value.split("\\s+", 2);
            if (parts.length == 2 && StrUtil.isNotBlank(parts[0])) {
                return parts[0] + " " + mask(parts[1]);
            }
        }
        return mask(value);
    }

    private static String mask(String value) {
        if (StrUtil.isBlank(value)) {
            return "***";
        }
        if (value.length() <= 6) {
            return "***";
        }
        return value.substring(0, 3) + "***" + value.substring(value.length() - 2);
    }
}
