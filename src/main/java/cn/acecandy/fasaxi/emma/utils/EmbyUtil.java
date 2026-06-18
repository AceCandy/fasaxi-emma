package cn.acecandy.fasaxi.emma.utils;

import cn.acecandy.fasaxi.emma.common.vo.HeadVO;
import cn.acecandy.fasaxi.emma.common.vo.HttpReqVO;
import cn.acecandy.fasaxi.emma.config.EmbyConfig;
import cn.acecandy.fasaxi.emma.config.OpConfig;
import cn.acecandy.fasaxi.emma.config.TmdbConfig;
import cn.acecandy.fasaxi.emma.sao.proxy.TmdbProxy;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.io.file.FileUtil;
import cn.hutool.v7.core.map.MapUtil;
import cn.hutool.v7.core.net.url.UrlEncoder;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.http.client.Request;
import cn.hutool.v7.http.client.Response;
import cn.hutool.v7.http.client.engine.ClientEngine;
import cn.hutool.v7.http.meta.Method;
import cn.hutool.v7.json.JSONObject;
import cn.hutool.v7.json.JSONUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static cn.hutool.v7.http.meta.HttpStatus.HTTP_MOVED_TEMP;

/**
 * emby 工具类
 *
 * @author tangningzhu
 * @since 2024/10/16
 */
@Slf4j
@Component
public class EmbyUtil {
    private static final Map<String, String> PATH_ALIST_BASE_CONFIG = MapUtil.<String, String>builder()
            .put("Z:\\", "/")
            .put("100PB:", "/115")
            .build();
    @Resource
    private ClientEngine httpClient;
    @Resource
    private EmbyConfig embyConfig;
    @Resource
    private OpConfig opConfig;
    @Resource
    private TmdbConfig tmdbConfig;
    @Resource
    private TmdbProxy tmdbProxy;

    /**
     * 解析头
     * <p>
     * // Header: Accept, Value: *\/*
     * // Header: Connection, Value: close
     * // Header: User-Agent, Value: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0
     * // Header: X-Forwarded-Proto, Value: http
     * // Header: Sec-Fetch-Site, Value: same-origin
     * // Header: Sec-Fetch-Dest, Value: video
     * // Header: Host, Value: 192.168.31.184:8801
     * // Header: Accept-Encoding, Value: identity;q=1, *;q=0
     * // Header: DNT, Value: 1
     * // Header: Pragma, Value: no-cache
     * // Header: Range, Value: bytes=0-
     * // Header: Sec-Fetch-Mode, Value: no-cors
     * // Header: sec-ch-ua, Value: "Microsoft Edge";v="131", "Chromium";v="131", "Not_A Brand";v="24"
     * // Header: sec-ch-ua-mobile, Value: ?0
     * // Header: Cache-Control, Value: no-cache
     * // Header: sec-ch-ua-platform, Value: "macOS"
     * // Header: X-Forwarded-For, Value: 192.168.97.1
     * // Header: Accept-Language, Value: zh-CN,zh;q=0.9,en;q=0.8
     * // Header: X-Real-IP, Value: 192.168.97.1
     *
     * @param request 请求
     * @return {@link HeadVO }
     */
    public static HeadVO parseHead(HttpServletRequest request) {
        return HeadVO.builder().ua(request.getHeader("User-Agent")).build();
    }

    /**
     * 解析http 请求入参
     *
     * @param request 请求
     * @return {@link HttpReqVO }
     */
    public static HttpReqVO parseHttpReq(HttpServletRequest request) {
        return HttpReqVO.builder()
                .requestUri(request.getRequestURI()).method(request.getMethod())
                .headers(rebuildReqHeader(request)).ua(request.getHeader("User-Agent"))
                .paramsDict(request.getParameterMap())
                .build();
    }

    public String replacePath2Alist(String inputPath) {
        if (StrUtil.isBlank(inputPath)) {
            return "";
        }
        Map<String, String> aliasConfig = buildPathAliasConfig();
        String bestMatchKey = aliasConfig.keySet().stream()
                .filter(prefix -> StrUtil.startWithIgnoreCase(inputPath, prefix))
                .max(Comparator.comparingInt(String::length))
                .orElse(null);
        if (bestMatchKey == null) {
            return "";
        }
        String changePath = StrUtil.replaceIgnoreCase(inputPath, bestMatchKey, aliasConfig.get(bestMatchKey));
        return normalizeAlistRelativePath(changePath);
    }

    private Map<String, String> buildPathAliasConfig() {
        Map<String, String> aliasConfig = new LinkedHashMap<>(PATH_ALIST_BASE_CONFIG.size() + 4);
        aliasConfig.putAll(PATH_ALIST_BASE_CONFIG);
        Stream.of(
                        appendDownloadPathPrefix(embyConfig.getAlistPublic()),
                        appendDownloadPathPrefix(embyConfig.getAlistInner()),
                        normalizeDownloadHost(opConfig.getDHost())
                )
                .filter(StrUtil::isNotBlank)
                .forEach(prefix -> aliasConfig.put(prefix, "/"));
        return aliasConfig;
    }

    private String appendDownloadPathPrefix(String host) {
        if (StrUtil.isBlank(host)) {
            return "";
        }
        String normalizedHost = StrUtil.removeSuffix(host, "/");
        if (StrUtil.endWithIgnoreCase(normalizedHost, "/d")) {
            return normalizedHost;
        }
        return normalizedHost + "/d";
    }

    private String normalizeDownloadHost(String host) {
        if (StrUtil.isBlank(host)) {
            return "";
        }
        return StrUtil.removeSuffix(host, "/");
    }

    private String normalizeAlistRelativePath(String path) {
        if (StrUtil.isBlank(path)) {
            return "";
        }
        String normalizedPath = FileUtil.normalize(path).replace('\\', '/');
        normalizedPath = StrUtil.replace(normalizedPath, "//", "/");
        return StrUtil.startWith(normalizedPath, "/") ? normalizedPath : "/" + normalizedPath;
    }

    public String buildAlistPath(String embyUri) {
        if (StrUtil.hasBlank(embyUri, embyConfig.getAlistPublic())) {
            return "";
        }
        return embyConfig.getAlistPublic() + "/d" + UrlEncoder.encodeQuery(embyUri);
    }

    /**
     * 重新设置转换请求头
     * <p>
     * 忽略host参数
     *
     * @param request 请求
     * @return {@link HttpHeaders }
     */
    public static HttpHeaders rebuildReqHeader(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();

        Map<String, String> headerMap = MapUtil.newHashMap();
        for (String headerName : Collections.list(request.getHeaderNames())) {
            if (!"host".equalsIgnoreCase(headerName)) {
                headerMap.put(headerName, request.getHeader(headerName));
            }
        }
        headers.setAll(headerMap);
        return headers;
    }

    /**
     * 获取媒体信息
     *
     * @param ua            ua
     * @param mediaSourceId 媒体源id
     * @return {@link String }
     */
    @SneakyThrows
    public String getItemInfo(String ua, String mediaSourceId) {
        String embyPath = fetchEmbyFilePath(mediaSourceId);
        String embyPathRes = replacePath2Alist(embyPath);
        if (StrUtil.isBlank(embyPathRes)) {
            return "";
        }
        var alistPath = buildAlistPath(embyPathRes);
        // 临时切换为bt 正常时可以注释掉
        alistPath = StrUtil.replaceFirst(alistPath, "micu-bt", "micu-pt", true);

        try (Response res = httpClient.send(Request.of(alistPath).method(Method.HEAD).header("User-Agent", ua)).sync()) {
            if (res.getStatus() == HTTP_MOVED_TEMP) {
                return CollUtil.getFirst(res.headers().get("Location"));
            }
        }
        return "";
    }

    /**
     * 获取Emby文件实际路径
     *
     * @param mediaSourceId 媒体源id
     * @return {@link String }
     */
    @SneakyThrows
    public String fetchEmbyFilePath(String mediaSourceId) {
        if (StrUtil.hasBlank(mediaSourceId, embyConfig.getHost(), embyConfig.getApiKey())) {
            return "";
        }
        try (Response res = httpClient.send(Request.of(embyConfig.getHost() + "/Items").method(Method.GET)
                .form(MapUtil.<String, Object>builder("Fields", "Path,MediaSources").put("Ids", mediaSourceId)
                        .put("Limit", 1).put("api_key", embyConfig.getApiKey()).map())).sync()) {
            if (res.isOk() && JSONUtil.isTypeJSON(res.bodyStr())) {
                JSONObject resJn = JSONUtil.parseObj(res.bodyStr());
                String mediaPath = resJn.getJSONArray("Items").getJSONObject(0)
                        .getJSONArray("MediaSources").getJSONObject(0)
                        .getStr("Path");
                // 外网转为内网
                return mediaPath;
                // return StrUtil.replaceIgnoreCase(mediaPath,
                //         embyConfig.getAlistPublic(), embyConfig.getAlistInner());
            }
        }
        return "";
    }

}
