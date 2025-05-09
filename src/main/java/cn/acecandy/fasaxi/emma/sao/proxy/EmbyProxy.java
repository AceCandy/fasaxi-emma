package cn.acecandy.fasaxi.emma.sao.proxy;

import cn.acecandy.fasaxi.emma.common.enums.EmbyPicType;
import cn.acecandy.fasaxi.emma.common.ex.BaseException;
import cn.acecandy.fasaxi.emma.common.resp.EmbyCachedResp;
import cn.acecandy.fasaxi.emma.config.EmbyConfig;
import cn.acecandy.fasaxi.emma.config.EmbyContentCacheReqWrapper;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.out.EmbyItem;
import cn.acecandy.fasaxi.emma.sao.out.EmbyItemsInfoOut;
import cn.acecandy.fasaxi.emma.sao.out.EmbyRemoteImageOut;
import cn.acecandy.fasaxi.emma.sao.out.TmdbImageInfoOut;
import cn.acecandy.fasaxi.emma.utils.CompressUtil;
import cn.acecandy.fasaxi.emma.utils.LockUtil;
import cn.acecandy.fasaxi.emma.utils.ReUtil;
import cn.acecandy.fasaxi.emma.utils.SortUtil;
import cn.acecandy.fasaxi.emma.utils.ThreadUtil;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.compress.ZipUtil;
import org.dromara.hutool.core.exception.ExceptionUtil;
import org.dromara.hutool.core.lang.Console;
import org.dromara.hutool.core.map.MapUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.http.HttpUtil;
import org.dromara.hutool.http.client.Request;
import org.dromara.hutool.http.client.Response;
import org.dromara.hutool.http.client.body.ResponseBody;
import org.dromara.hutool.http.client.engine.ClientEngine;
import org.dromara.hutool.http.meta.Method;
import org.dromara.hutool.json.JSONUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_302;

/**
 * emby 代理服务
 *
 * @author AceCandy
 * @since 2025/5/3
 */
@Component
@Slf4j
public class EmbyProxy {

    @Resource
    private ClientEngine httpClient;

    @Resource
    private EmbyConfig embyConfig;
    @Autowired
    private RedisClient redisClient;

    /**
     * 获取项目信息
     *
     * @param mediaSourceId 媒体源id
     * @return {@link TmdbImageInfoOut }
     */
    public EmbyItem getItemInfo(String mediaSourceId) {
        String url = embyConfig.getHost() + embyConfig.getItemInfoUrl();
        try (Response res = httpClient.send(Request.of(url).method(Method.GET)
                .form(MapUtil.<String, Object>builder("Fields", "Path,MediaSources,ProviderIds")
                        .put("Ids", mediaSourceId).put("Limit", 1)
                        .put("api_key", embyConfig.getApiKey()).map()))) {
            if (!res.isOk()) {
                throw new BaseException(StrUtil.format("返回码异常[{}]: {}", res.getStatus(), url));
            }
            String resBody = res.bodyStr();
            if (!JSONUtil.isTypeJSON(resBody)) {
                throw new BaseException(StrUtil.format("返回结果异常[{}]: {}", url, resBody));
            }
            return CollUtil.getFirst(JSONUtil.toBean(resBody, EmbyItemsInfoOut.class).getItems());
        } catch (Exception e) {
            log.warn("getItemInfo 网络请求异常: ", e);
        }
        return null;
    }

    /**
     * 获取远程图片
     *
     * @param itemId 媒体源id
     * @return {@link EmbyRemoteImageOut }
     */
    public EmbyRemoteImageOut.Img getRemoteImage(String itemId, EmbyPicType picType) {
        String url = embyConfig.getHost() + StrUtil.format(embyConfig.getRemoteImagesUrl(), itemId);
        try (Response res = httpClient.send(Request.of(url).method(Method.GET)
                .form(MapUtil.<String, Object>builder("api_key", embyConfig.getApiKey()).put("Limit", 1)
                        .put("ProviderName", "TheMovieDb").put("Type", picType.getValue()).map()))) {
            if (!res.isOk()) {
                throw new BaseException(StrUtil.format("返回码异常[{}]: {}", res.getStatus(), url));
            }
            String resBody = res.bodyStr();
            if (!JSONUtil.isTypeJSON(resBody)) {
                throw new BaseException(StrUtil.format("返回结果异常[{}]: {}", url, resBody));
            }
            return CollUtil.getFirst(JSONUtil.toBean(resBody, EmbyRemoteImageOut.class).getImages());
        } catch (Exception e) {
            log.warn("getRemoteImage 网络请求异常: ", e);
        }
        return null;
    }

    /**
     * 刷新tmdb数据
     *
     * @param itemId 媒体源id
     */
    public void refresh(String itemId) {
        String url = embyConfig.getHost() + StrUtil.format(embyConfig.getRefreshUrl(), itemId);
        Map<String, Object> paramMap = MapUtil.<String, Object>builder("api_key", embyConfig.getApiKey())
                .put("imageRefreshMode", "FullRefresh").put("metadataRefreshMode", "FullRefresh")
                .put("recursive", true).put("replaceAllImages", "true")
                .put("replaceAllMetadata", true).map();
        url = HttpUtil.urlWithFormUrlEncoded(url, paramMap, Charset.defaultCharset());
        try (Response res = httpClient.send(Request.of(url).method(Method.POST))) {
            if (!res.isOk()) {
                throw new BaseException(StrUtil.format("返回码异常[{}]: {}", res.getStatus(), url));
            }
            log.warn("刷新tmdb数据: {}", itemId);
        } catch (Throwable e) {
            if (StrUtil.contains(ExceptionUtil.getSimpleMessage(e), "Cannot invoke " +
                    "\"org.apache.hc.core5.http.HttpEntity.getContent()\" because \"this.entity\" is null")) {
                log.warn("刷新tmdb数据: {}", itemId);
            } else {
                log.warn("getRemoteImage 网络请求异常: ", e);
            }
        }
    }

    /**
     * 获取302的真实路径
     *
     * @param mediaPath 媒体路径
     * @param header    头球
     * @return {@link String }
     */
    @SneakyThrows
    public String fetch302Path(String mediaPath, Map<String, String> header) {
        if (StrUtil.isBlank(mediaPath)) {
            return null;
        }
        try (Response res = httpClient.send(Request.of(mediaPath).method(Method.HEAD).header(header))) {
            if (res.getStatus() == CODE_302) {
                return res.header("Location");
            }
        }
        return null;
    }

    @SneakyThrows
    public EmbyCachedResp transferResp(Response res, EmbyContentCacheReqWrapper request) {
        EmbyCachedResp embyCachedResp = new EmbyCachedResp();
        embyCachedResp.setStatusCode(res.getStatus());
        if (!res.isOk()) {
            return embyCachedResp;
        }
        res.headers().forEach((k, v) -> {
            if (k == null || StrUtil.equalsIgnoreCase(k, "content-length")) {
                return;
            }
            embyCachedResp.getHeaders().put(k, StrUtil.join(StrUtil.COMMA, v));
        });
        ResponseBody body = res.body().sync();
        if (StrUtil.equalsAnyIgnoreCase(request.getMethod(), "get") && StrUtil.containsIgnoreCase(
                embyCachedResp.getHeaders().get("Content-Type"), "application/json")) {
            String content = "";
            if (StrUtil.equalsIgnoreCase(embyCachedResp.getHeaders().get("Content-Encoding"), "br")) {
                String bodyStr = new String(CompressUtil.decode(body.getBytes()));
                content = changeRespBody(request, bodyStr);
            } else if (StrUtil.containsIgnoreCase(embyCachedResp.getHeaders().get("Content-Encoding"), "gzip")) {
                String bodyStr = new String(ZipUtil.unGzip(body.getBytes()));
                content = changeRespBody(request, bodyStr);
            } else {
                String bodyStr = body.getString();
                content = changeRespBody(request, bodyStr);
            }
            embyCachedResp.setContent(content.getBytes());
        } else {
            embyCachedResp.setContent(body.getBytes());
        }
        return embyCachedResp;
    }

    private String changeRespBody(EmbyContentCacheReqWrapper request, String bodyStr) {
        refreshItem(request, bodyStr);
        bodyStr = searchItem(request, bodyStr);
        return StrUtil.replaceIgnoreCase(bodyStr, "micu", "REDMT");
    }


    /**
     * 返回结果个性化排序
     *
     * @param request 要求
     * @param bodyStr 身体str
     */
    private String searchItem(EmbyContentCacheReqWrapper request, String bodyStr) {
        if (!ReUtil.isItemsUrl(request.getRequestURI().toLowerCase()) ||
                !request.getCachedParam().containsKey("searchterm")) {
            return bodyStr;
        }
        EmbyItemsInfoOut itemInfo = JSONUtil.toBean(bodyStr, EmbyItemsInfoOut.class);
        if (CollUtil.isEmpty(itemInfo.getItems())) {
            return bodyStr;
        }
        List<EmbyItem> items = SortUtil.searchSortItem(itemInfo.getItems(),
                request.getCachedParam().get("searchterm").toString());
        itemInfo.setItems(items);
        return JSONUtil.toJsonStr(itemInfo);
    }

    /**
     * 刷新项目
     *
     * @param request 要求
     * @param bodyStr 身体str
     */
    private void refreshItem(EmbyContentCacheReqWrapper request, String bodyStr) {
        if (CollUtil.isEmpty(ReUtil.isItemUrl(request.getRequestURI().toLowerCase()))) {
            return;
        }
        ThreadUtil.execVirtual(() -> {
            EmbyItem item = JSONUtil.toBean(bodyStr, EmbyItem.class);
            if (!StrUtil.containsAny(item.getUniqueKey(), "tmdb", "tt", "zh-CN-cf")
                    || StrUtil.isNotBlank(item.getImageTags().getPrimary())) {
                return;
            }
            String lockKey = LockUtil.buildRefreshLock(item.getItemId());
            if (redisClient.get(lockKey) != null) {
                return;
            }
            try {
                refresh(item.getItemId());
            } finally {
                redisClient.set(lockKey, "1", 2 * 60 * 60);
            }
        });
    }

    public static void main(String[] args) {
        String param = "Recursive=true&ImageRefreshMode=FullRefresh&MetadataRefreshMode=FullRefresh&ReplaceAllImages=false&ReplaceAllMetadata=true&X-Emby-Client=Emby+Web&X-Emby-Device-Name=Microsoft+Edge+macOS&X-Emby-Device-Id=6a95e0b8-b44e-450e-bf64-6090e799000a&X-Emby-Client-Version=4.9.0.42&X-Emby-Token=e2262107a13c45a7bfc48884be6f98ad&X-Emby-Language=zh-cn";
        Map<String, Object> paramMap = MapUtil.<String, Object>builder("X-Emby-Token", "b8647127d2fa4ae6b27b6918ed8a0593")
                .put("imageRefreshMode", "FullRefresh").put("metadataRefreshMode", "FullRefresh")
                .put("recursive", true).put("replaceAllImages", "true")
                .put("api_key", "e2262107a13c45a7bfc48884be6f98ad")
                .put("replaceAllMetadata", true).map();
        // String res = HttpUtil.post("https://emby-real.acecandy.cn:880/emby/Items/1417549/Refresh", param);
        // Console.log(res);
        Console.log(HttpUtil.createClient("JdkClientEngine").send(HttpUtil.createPost(HttpUtil.urlWithFormUrlEncoded("https://emby-real.acecandy.cn:880/emby/Items/1417549/Refresh", paramMap, Charset.defaultCharset()))).getStatus());

    }
}