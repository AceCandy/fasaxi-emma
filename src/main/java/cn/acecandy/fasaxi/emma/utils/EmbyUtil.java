package cn.acecandy.fasaxi.emma.utils;

import cn.acecandy.fasaxi.emma.common.vo.HeadVO;
import cn.acecandy.fasaxi.emma.common.vo.HttpReqVO;
import cn.acecandy.fasaxi.emma.config.FastEmbyConfig;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import static io.undertow.util.StatusCodes.FOUND;
import static org.springframework.http.HttpStatus.OK;

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
    private CloseableHttpClient embyHttpClient;

    @Resource
    private FastEmbyConfig fastEmbyConfig;

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
    public String getItemInfo(String ua, String mediaSourceId) {
        String embyPath = fetchEmbyFilePath(mediaSourceId);
        String embyPathRes = replacePath2Alist(embyPath);
        if (StrUtil.isBlank(embyPathRes)) {
            return "";
        }
        var alistPath = buildAlistPath(embyPathRes);
        // 临时切换为bt 正常时可以注释掉
        alistPath = StrUtil.replaceFirst(alistPath, "micu-bt", "micu-pt");
        try (HttpResponse res = HttpRequest.head(alistPath).header("User-Agent", ua).execute()) {
            if (res.getStatus() == HttpStatus.HTTP_MOVED_TEMP) {
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
    public String fetchEmbyFilePath(String mediaSourceId) {
        if (StrUtil.isBlank(mediaSourceId)) {
            return "";
        }
        try (HttpResponse res = HttpRequest.get(fastEmbyConfig.getHost() + "/Items")
                .form("Fields", "Path,MediaSources")
                .form("Ids", mediaSourceId).form("Limit", 1)
                .form("api_key", EMBY_API_KEY).timeout(10 * 1000)
                .execute()) {
            if (res.isOk() && JSONUtil.isTypeJSON(res.body())) {
                JSONObject resJn = JSONUtil.parseObj(res.body());
                String mediaPath = resJn.getJSONArray("Items").getJSONObject(0)
                        .getJSONArray("MediaSources").getJSONObject(0)
                        .getStr("Path");
                // 外网转为内网
                return StrUtil.replaceIgnoreCase(mediaPath,
                        fastEmbyConfig.getAlistPublic(), fastEmbyConfig.getAlistInner());
            }
        }
        return "";
    }

    /**
     * 获取302的真实路径
     *
     * @param mediaPath 媒体路径
     * @param header    头球
     * @return {@link String }
     */
    public String fetch302Path(String mediaPath, Map<String, String> header) {
        if (StrUtil.isBlank(mediaPath)) {
            return "";
        }
        try (HttpResponse res = HttpUtil.createRequest(Method.HEAD, mediaPath)
                .headerMap(header, true).execute()) {
            if (res.getStatus() == FOUND) {
                return res.header("Location");
            }
        }
        return "";
    }

    public FileCacheUtil.FileInfo getFileInfo(Long mediaSourceId) {
        try (HttpResponse res = HttpRequest.get(fastEmbyConfig.getHost() + "/Items")
                .form("Fields", "Path,MediaSources")
                .form("Ids", mediaSourceId).form("Limit", 1)
                .form("api_key", EMBY_API_KEY).timeout(10 * 1000)
                .execute()) {
            if (res.isOk() && JSONUtil.isTypeJSON(res.body())) {
                JSONObject resJn = JSONUtil.parseObj(res.body());
                JSONObject item = resJn.getJSONArray("Items").getJSONObject(0);
                String mediaPath = item.getJSONArray("MediaSources").getJSONObject(0)
                        .getStr("Path");
                // 外网转为内网
                mediaPath = StrUtil.replaceIgnoreCase(mediaPath,
                        fastEmbyConfig.getAlistPublic(), fastEmbyConfig.getAlistInner());
                return FileCacheUtil.FileInfo.builder()
                        .itemId(NumberUtil.parseLong(item.getStr("Id"))).path(mediaPath)
                        .itemType(item.getStr("Type", "未知"))
                        .seasonId(item.getLong("SeasonId"))
                        .bitrate(item.getLong("Bitrate", 27962026L))
                        .size(item.getLong("Size", 0L))
                        .container(item.getStr("Container"))
                        .cacheFileSize(item.getLong("Bitrate", 27962026L) / 8 * 15)
                        .build();
            }
        }
        return FileCacheUtil.FileInfo.builder().build();
    }


    public static String replacePath2Alist(String inputPath) {
        for (Map.Entry<String, String> entry : PATH_ALIST_CONFIG.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            if (!inputPath.startsWith(k)) {
                continue;
            }
            String changePath = StrUtil.replace(inputPath, k, v);
            return StrUtil.replace(URLUtil.decode(changePath), "\\", "/");
        }
        return "";
    }

    public static String buildAlistPath(String embyUri) {
        if (StrUtil.isBlank(embyUri)) {
            return "";
        }
        return ALIST_PUBLIC_ADDR + "/d" + URLUtil.encode(embyUri);
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
     * 重建 请求出参头
     *
     * @param resp 请求出参
     * @return {@link HttpHeaders }
     */
    public static HttpHeaders rebuildRespHeader(HttpResponse resp) {
        HttpHeaders headers = new HttpHeaders();

        Map<String, String> headerMap = MapUtil.newHashMap();
        resp.headers().forEach((k, v) -> {
            if (k != null) {
                headerMap.put(k, CollUtil.getFirst(v));
            }
        });
        headers.setAll(headerMap);
        return headers;
    }


    /**
     * 代理直接请求
     *
     * @param originalUrl 原始url
     * @param httpReqVO   http 请求入参vo
     * @return {@link ResponseEntity }<{@link ? }>
     */
    public static ResponseEntity<?> proxyRequest(String originalUrl, HttpReqVO httpReqVO) {
        try {
            HttpRequest proxyRequest = HttpUtil
                    .createRequest(Method.valueOf(httpReqVO.getMethod()), originalUrl).timeout(2000);
            proxyRequest.header(httpReqVO.getHeaders());
            httpReqVO.getParamsDict().forEach(proxyRequest::form);
            try (HttpResponse httpResponse = proxyRequest.executeAsync()) {
                return ResponseEntity.status(httpResponse.getStatus())
                        .headers(EmbyUtil.rebuildRespHeader(httpResponse))
                        .body(httpResponse.body());
            }
        } catch (Exception e) {
            log.error("代理直接请求异常:{}", originalUrl, e);
            return ResponseEntity.status(OK).body("Proxy error: " + e.getMessage());
        }
    }

    /**
     * 302重定向
     *
     * @param redirectUrl 重定向url
     * @return {@link ResponseEntity }<{@link ? }>
     */
    public static ResponseEntity<?> redirect302(String ua, String mediaSourceId, String redirectUrl) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", redirectUrl);
            log.info("重定向到: {}", redirectUrl);
            return new ResponseEntity<>(headers, org.springframework.http.HttpStatus.FOUND);
        } finally {
            if (!StrUtil.contains(redirectUrl, "cdnfhnfile.115.com")) {
                ua = "";
            }
            CacheUtil.setMediaKey(ua, mediaSourceId, redirectUrl);
        }
    }

    public static class Result {
        String subdirectory;
        String hash;

        public Result(String subdirectory, String hash) {
            this.subdirectory = subdirectory;
            this.hash = hash;
        }

        public String getSubdirectory() {
            return subdirectory;
        }

        public String getHash() {
            return hash;
        }
    }

    public static void main(String[] args) {
        String filePath = "/Users/mac/Downloads/2024-10-12 10.21.14.mp4";
        Path path = Paths.get(filePath);
        Console.log(FileUtil.subPath(path, path.getNameCount() - 3, path.getNameCount()));
        Console.log(StrUtil.subPre(filePath, 2));
    }
}