package cn.acecandy.fasaxi.emma.service;

import cn.acecandy.fasaxi.emma.common.req.VideoRedirectReq;
import cn.acecandy.fasaxi.emma.common.resp.EmbyItemsResp;
import cn.acecandy.fasaxi.emma.common.vo.HttpReqVO;
import cn.acecandy.fasaxi.emma.config.FastEmbyConfig;
import cn.acecandy.fasaxi.emma.utils.CacheUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

import static cn.acecandy.fasaxi.emma.utils.EmbyUtil.proxyRequest;
import static cn.acecandy.fasaxi.emma.utils.EmbyUtil.redirect302;

/**
 * FastEmby服务
 *
 * @author AceCandy
 * @since 2024/11/19
 */
@Slf4j
@Component
public class FastEmbyService {

    @Resource
    private FastEmbyConfig fastEmbyConfig;

    public ResponseEntity<?> handleEmbyRequest(HttpReqVO httpReqVO,
                                               String videoId, VideoRedirectReq req) {
        String originalUrl = fastEmbyConfig.getHost() + httpReqVO.getRequestUri();
        try {
            if (StrUtil.containsIgnoreCase(httpReqVO.getRequestUri(),"subtitles")) {
                return proxyRequest(originalUrl, httpReqVO);
            }

            String ua = httpReqVO.getUa();
            String mediaSourceId = StrUtil.isBlank(req.getMediaSourceId()) ? videoId : req.getMediaSourceId();

            String redirectUrl = CacheUtil.getMediaKey(ua, mediaSourceId);
            if (StrUtil.isBlank(redirectUrl)) {
                String embyFilePath = fetchEmbyFilePath(mediaSourceId, req.getApi_key());
                redirectUrl = findRedirectUrl(embyFilePath, ua);
            }
            return redirect302(ua, mediaSourceId, redirectUrl);
        } catch (Exception e) {
            log.error("处理Emby请求失败:{} ", originalUrl, e);
            return proxyRequest(originalUrl, httpReqVO);
        }
    }

    /**
     * 重定向
     *
     * @param embyFilePath emby文件路径
     * @param ua           ua
     * @return {@link String 重定向url}
     */
    private static String findRedirectUrl(String embyFilePath, String ua) {
        // TODO 特殊处理
        try (HttpResponse headResponse = HttpUtil.createRequest(Method.HEAD, embyFilePath)
                .header("User-Agent", ua).executeAsync()) {
            String redirectUrl = headResponse.header("Location");

            if (headResponse.getStatus() != HttpStatus.HTTP_MOVED_TEMP ||
                    StrUtil.isBlank(redirectUrl) || StrUtil.equalsIgnoreCase(redirectUrl, embyFilePath)) {
                throw new RuntimeException("请求302 URL失败，请检查路径替换是否成功，以及cookies是否失效");
            }
            return redirectUrl;
        }
    }

    /**
     * 获取emby文件路径
     *
     * @param mediaSourceId 媒体源id
     * @param apiKey        api密钥
     * @return {@link String }
     */
    public String fetchEmbyFilePath(String mediaSourceId, String apiKey) {
        String redirectUrl = StrUtil.format("{}/Items?Ids={}&Fields=Path,MediaSources&Limit=1&api_key={}",
                fastEmbyConfig.getHost(), mediaSourceId, apiKey);
        try (HttpResponse res = HttpRequest.get(redirectUrl).timeout(2000).executeAsync()) {
            if (!res.isOk() || StrUtil.isBlank(res.body())) {
                throw new RuntimeException("获取emby-items失败");
            }
            EmbyItemsResp embyItem = JSONUtil.toBean(res.body(), EmbyItemsResp.class);
            String embyFilePath = CollUtil.getFirst(CollUtil.getFirst(embyItem.getItems()).getMediaSources()).getPath();

            embyFilePath = URLUtil.decode(embyFilePath);
            log.info("获取到Emby-strm路径: {}", embyFilePath);

            if (!isStrmFilePath(embyFilePath)) {
                throw new RuntimeException("不属于strm类型文件路径");
            }
            embyFilePath = replaceFilePathMapping(embyFilePath);
            log.info("转换后的Emby-strm路径: {}", embyFilePath);
            return embyFilePath;
        }
    }


    /**
     * 通过mapping修改strm文件
     *
     * @param filepath 文件路径
     * @return {@link String }
     */
    public static String replaceFilePathMapping(String filepath) {
        /*List<String> filePathMapping = CollUtil.newArrayList();
        String oldPath = filepath;
        for (String item : filePathMapping) {
            try {
                String[] parts = item.split(" => ");
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Invalid mapping format");
                }
                String pattern = parts[0];
                String replacement = parts[1].equals("\"\"") ? "" : parts[1];
                filepath = filepath.replace(pattern, replacement);
            } catch (Exception e) {
                logger.severe("路径替换失败,当前路径规则为 ::: " + item + " => " + e);
            }
        }
        filepath = filepath.replace("\\", "/");
        logger.info("源路径已替换: " + oldPath + " => " + filepath);*/
        return filepath;
    }

    /**
     * 是strm的文件路径
     *
     * @param filePath 文件路径
     * @return boolean
     */
    private boolean isStrmFilePath(String filePath) {
        List<String> embyStrmPaths = fastEmbyConfig.getStrmPaths();
        if (CollUtil.isEmpty(embyStrmPaths)) {
            return false;
        }
        for (String path : embyStrmPaths) {
            if (StrUtil.startWithIgnoreCase(filePath, path)) {
                return true;
            }
        }
        return false;
    }
}