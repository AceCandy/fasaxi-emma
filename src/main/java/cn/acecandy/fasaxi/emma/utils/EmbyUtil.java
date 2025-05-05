package cn.acecandy.fasaxi.emma.utils;

import cn.acecandy.fasaxi.emma.common.vo.HeadVO;
import cn.acecandy.fasaxi.emma.common.vo.HttpReqVO;
import cn.acecandy.fasaxi.emma.config.EmbyConfig;
import cn.acecandy.fasaxi.emma.config.TmdbConfig;
import cn.acecandy.fasaxi.emma.sao.proxy.TmdbProxy;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.map.MapUtil;
import org.dromara.hutool.core.math.NumberUtil;
import org.dromara.hutool.core.net.url.UrlEncoder;
import org.dromara.hutool.core.net.url.UrlUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.http.client.Request;
import org.dromara.hutool.http.client.Response;
import org.dromara.hutool.http.client.engine.ClientEngine;
import org.dromara.hutool.http.meta.Method;
import org.dromara.hutool.json.JSONObject;
import org.dromara.hutool.json.JSONUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

import static org.dromara.hutool.http.meta.HttpStatus.HTTP_MOVED_TEMP;

/**
 * emby 工具类
 *
 * @author tangningzhu
 * @since 2024/10/16
 */
@Slf4j
@Component
public class EmbyUtil {
    @Resource
    private ClientEngine httpClient;

    @Resource
    private EmbyConfig embyConfig;

    @Resource
    private TmdbConfig tmdbConfig;

    @Resource
    private TmdbProxy tmdbProxy;

    private static final String EMBY_INNER_URL = "http://192.168.1.205:8096";
    private static final String EMBY_PUBLIC_URL = "http://emby-real.acecandy.cn:800";
    private static final String EMBY_API_KEY = "b8647127d2fa4ae6b27b6918ed8a0593";
    private static final String ALIST_ADDR = "http://192.168.1.205:5244";
    private static final String ALIST_PUBLIC_ADDR = "https://alist.acecandy.cn:880";
    private static final String ALIST_TOKEN =
            "alist-1fbc1ca5-4506-49d8-9950-53d9ab7edefaNH7xJwk7OYxNvaEP2Vd5fd9lpIBLanXcfNFZHtMvMQ73YfL4Z0ojIFwAjbFGsccd";

    private static final Map<String, String> PATH_ALIST_CONFIG = MapUtil.<String, String>builder()
            .put("Z:\\", "/")
            .put("100PB:", "/115")
            .put("https://alist.acecandy.cn:880/d/", "/")
            .put("http://192.168.1.205:5244/d", "/")
            .build();

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
        if (StrUtil.isBlank(mediaSourceId)) {
            return "";
        }
        try (Response res = httpClient.send(Request.of(embyConfig.getHost() + "/Items").method(Method.GET)
                .form(MapUtil.<String, Object>builder("Fields", "Path,MediaSources").put("Ids", mediaSourceId)
                        .put("Limit", 1).put("api_key", EMBY_API_KEY).map())).sync()) {
            if (res.isOk() && JSONUtil.isTypeJSON(res.bodyStr())) {
                JSONObject resJn = JSONUtil.parseObj(res.bodyStr());
                String mediaPath = resJn.getJSONArray("Items").getJSONObject(0)
                        .getJSONArray("MediaSources").getJSONObject(0)
                        .getStr("Path");
                // 外网转为内网
                return StrUtil.replaceIgnoreCase(mediaPath,
                        embyConfig.getAlistPublic(), embyConfig.getAlistInner());
            }
        }
        return "";
    }



    @SneakyThrows
    public FileCacheUtil.FileInfo getFileInfo(Long mediaSourceId) {
        try (Response res = httpClient.send(Request.of(embyConfig.getHost() + "/Items").method(Method.GET)
                .form(MapUtil.<String, Object>builder("Fields", "Path,MediaSources").put("Ids", mediaSourceId)
                        .put("Limit", 1).put("api_key", EMBY_API_KEY).map()))) {
            if (!res.isOk()) {
                return null;
            }
            String resBody = res.bodyStr();
            if (res.isOk() && JSONUtil.isTypeJSON(resBody)) {
                JSONObject resJn = JSONUtil.parseObj(resBody);
                JSONObject item = resJn.getJSONArray("Items").getJSONObject(0);
                String mediaPath = item.getJSONArray("MediaSources").getJSONObject(0)
                        .getStr("Path");
                // 外网转为内网
                mediaPath = StrUtil.replaceIgnoreCase(mediaPath,
                        embyConfig.getAlistPublic(), embyConfig.getAlistInner());
                Long minute = item.getLong("RunTimeTicks", 0L) / 10000 / 1000 / 60;
                Long cacheFileSize = item.getLong("Size", 0L) / minute;
                return FileCacheUtil.FileInfo.builder()
                        .itemId(NumberUtil.parseLong(item.getStr("Id"))).path(mediaPath)
                        .itemType(item.getStr("Type", "未知"))
                        .seasonId(item.getLong("SeasonId"))
                        .bitrate(item.getLong("Bitrate", 27962026L))
                        .size(item.getLong("Size", 0L))
                        .container(item.getStr("Container"))
                        .cacheFileSize(cacheFileSize)
                        .build();
            }
        }
        return null;
    }

    public static String replacePath2Alist(String inputPath) {
        for (Map.Entry<String, String> entry : PATH_ALIST_CONFIG.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            if (!inputPath.startsWith(k)) {
                continue;
            }
            String changePath = StrUtil.replace(inputPath, k, v);
            return UrlUtil.normalize(changePath, true, true);
        }
        return "";
    }

    public static String buildAlistPath(String embyUri) {
        if (StrUtil.isBlank(embyUri)) {
            return "";
        }
        return ALIST_PUBLIC_ADDR + "/d" + UrlEncoder.encodeQuery(embyUri);
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

}