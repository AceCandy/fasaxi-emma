package cn.acecandy.fasaxi.emma.sao.proxy;

import cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType;
import cn.acecandy.fasaxi.emma.common.enums.EmbyPicType;
import cn.acecandy.fasaxi.emma.common.ex.BaseException;
import cn.acecandy.fasaxi.emma.common.resp.EmbyCachedResp;
import cn.acecandy.fasaxi.emma.config.EmbyConfig;
import cn.acecandy.fasaxi.emma.config.EmbyContentCacheReqWrapper;
import cn.acecandy.fasaxi.emma.dao.embyboss.entity.TmdbProvider;
import cn.acecandy.fasaxi.emma.dao.embyboss.service.TmdbProviderDao;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.out.EmbyItem;
import cn.acecandy.fasaxi.emma.sao.out.EmbyItemsInfoOut;
import cn.acecandy.fasaxi.emma.sao.out.EmbyMediaSource;
import cn.acecandy.fasaxi.emma.sao.out.EmbyPlaybackOut;
import cn.acecandy.fasaxi.emma.sao.out.EmbyRemoteImageOut;
import cn.acecandy.fasaxi.emma.sao.out.EmbyViewOut;
import cn.acecandy.fasaxi.emma.sao.out.TmdbImageInfoOut;
import cn.acecandy.fasaxi.emma.utils.CacheUtil;
import cn.acecandy.fasaxi.emma.utils.EmbyProxyUtil;
import cn.acecandy.fasaxi.emma.utils.LockUtil;
import cn.acecandy.fasaxi.emma.utils.ReUtil;
import cn.acecandy.fasaxi.emma.utils.SortUtil;
import cn.acecandy.fasaxi.emma.utils.ThreadUtil;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.collection.ListUtil;
import org.dromara.hutool.core.date.DateTime;
import org.dromara.hutool.core.exception.ExceptionUtil;
import org.dromara.hutool.core.map.MapUtil;
import org.dromara.hutool.core.net.url.UrlBuilder;
import org.dromara.hutool.core.net.url.UrlDecoder;
import org.dromara.hutool.core.net.url.UrlPath;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.http.HttpUtil;
import org.dromara.hutool.http.client.Request;
import org.dromara.hutool.http.client.Response;
import org.dromara.hutool.http.client.body.ResponseBody;
import org.dromara.hutool.http.client.engine.ClientEngine;
import org.dromara.hutool.http.meta.Method;
import org.dromara.hutool.http.server.servlet.ServletUtil;
import org.dromara.hutool.json.JSONArray;
import org.dromara.hutool.json.JSONObject;
import org.dromara.hutool.json.JSONUtil;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_204;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_302;
import static cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType.电影;
import static cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType.电视剧;
import static cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType.电视剧_集;
import static org.dromara.hutool.core.text.StrPool.COMMA;

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

    @Resource
    private RedisClient redisClient;

    @Resource
    private TmdbProviderDao tmdbProviderDao;

    @Resource
    private DoubanProxy doubanProxy;

    @Resource
    private TmdbProxy tmdbProxy;

    /**
     * 获取视图
     *
     * @param userId 用户ID
     * @return {@link List<EmbyItem> }
     */
    public EmbyViewOut getViews(String userId) {
        String url = embyConfig.getHost() + StrUtil.format(embyConfig.getViewsUrl(), userId);
        try (Response res = httpClient.send(Request.of(url).method(Method.GET)
                .form(MapUtil.<String, Object>builder("api_key", embyConfig.getApiKey()).map()))) {
            if (!res.isOk()) {
                throw new BaseException(StrUtil.format("返回码异常[{}]: {}", res.getStatus(), url));
            }
            String resBody = res.bodyStr();
            if (!JSONUtil.isTypeJSON(resBody)) {
                throw new BaseException(StrUtil.format("返回结果异常[{}]: {}", url, resBody));
            }
            return JSONUtil.toBean(resBody, EmbyViewOut.class);
        } catch (Exception e) {
            log.warn("getViews 网络请求异常: ", e);
        }
        return null;
    }

    /**
     * 获取视图
     *
     * @param userId 用户ID
     * @return {@link List<EmbyItem> }
     */
    public EmbyItemsInfoOut getUserItems(String userId, List<String> itemIds,
                                         List<String> sortBy, String sortOrder,
                                         Integer startIndex, Integer limit, String fields, String itemTypes) {
        String url = embyConfig.getHost() + StrUtil.format(embyConfig.getUserItemUrl(), userId);
        Map<String, Object> paramMap = MapUtil.<String, Object>builder("api_key", embyConfig.getApiKey())
                .put("Ids", StrUtil.join(COMMA, itemIds)).put("Fields", fields)
                .put("Recursive", true)
                .put("IncludeItemTypes", StrUtil.isNotBlank(itemTypes) ? itemTypes : "Movie,Series").build();
        if (CollUtil.isNotEmpty(sortBy)) {
            paramMap.put("SortBy", StrUtil.join(COMMA, sortBy));
        }
        if (StrUtil.isNotBlank(sortOrder)) {
            paramMap.put("SortOrder", sortOrder);
        }
        if (null != limit) {
            paramMap.put("Limit", limit);
        }
        if (null != startIndex) {
            paramMap.put("StartIndex", startIndex);
        }

        try (Response res = httpClient.send(Request.of(url).method(Method.GET).form(paramMap))) {
            if (!res.isOk()) {
                throw new BaseException(StrUtil.format("返回码异常[{}]: {}", res.getStatus(), url));
            }
            String resBody = res.bodyStr();
            if (!JSONUtil.isTypeJSON(resBody)) {
                throw new BaseException(StrUtil.format("返回结果异常[{}]: {}", url, resBody));
            }
            return JSONUtil.toBean(resBody, EmbyItemsInfoOut.class);
        } catch (Exception e) {
            log.warn("getUserItems 网络请求异常: ", e);
        }
        return null;
    }

    /**
     * 获取用户拥有权限的媒体项(未观看)
     *
     * @param userId 用户ID
     * @return {@link List<EmbyItem> }
     */
    public List<String> getUserPermsEmbyIdOnLock(String userId) {
        Lock lock = LockUtil.lockUserPerms(userId);
        if (LockUtil.isLock(lock)) {
            return ListUtil.of();
        }
        String url = embyConfig.getHost() + embyConfig.getItemInfoUrl();

        int start = 0;
        int batchSize = 5000;

        List<String> embyIds = ListUtil.of();
        try {
            while (true) {
                try (Response res = httpClient.send(Request.of(url).method(Method.GET).form(Map.of(
                        "api_key", embyConfig.getApiKey(), "Recursive", true,
                        "IncludeItemTypes", "Movie,Series", "UserId", userId,
                        "StartIndex", start, "Limit", batchSize,
                        "Fields", "Id")))) {
                    if (!res.isOk()) {
                        throw new BaseException(StrUtil.format("返回码异常[{}]: {}", res.getStatus(), url));
                    }
                    String resBody = res.bodyStr();
                    if (!JSONUtil.isTypeJSON(resBody)) {
                        throw new BaseException(StrUtil.format("返回结果异常[{}]: {}", url, resBody));
                    }
                    EmbyItemsInfoOut out = JSONUtil.toBean(resBody, EmbyItemsInfoOut.class);
                    List<EmbyItem> itemList = out.getItems();
                    embyIds.addAll(itemList.stream().filter(i ->
                                    !Boolean.parseBoolean(i.getUserData().get("Played").toString()))
                            .map(EmbyItem::getItemId).toList());
                    if (CollUtil.isEmpty(itemList) || CollUtil.size(itemList) < batchSize) {
                        break;
                    }
                    start += CollUtil.size(itemList);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (Exception e) {
            log.warn("getUserPermsEmbyId 网络请求异常: ", e);
        } finally {
            LockUtil.unlockUserPerms(lock, userId);
        }
        return embyIds;
    }

    /**
     * 获取媒体信息
     * <p>
     * 如果媒体信息不存在时会自动获取媒体信息
     *
     * @param mediaSourceId 媒体源id
     * @return {@link TmdbImageInfoOut }
     */
    public EmbyMediaSource getPlayback(String mediaSourceId) {
        String url = embyConfig.getHost() + StrUtil.format(embyConfig.getPlaybackUrl(), mediaSourceId);
        try (Response res = httpClient.send(Request.of(url).method(Method.GET)
                .form(MapUtil.<String, Object>builder("api_key", embyConfig.getApiKey()).map()))) {
            if (!res.isOk()) {
                throw new BaseException(StrUtil.format("返回码异常[{}]: {}", res.getStatus(), url));
            }
            String resBody = res.bodyStr();
            if (!JSONUtil.isTypeJSON(resBody)) {
                throw new BaseException(StrUtil.format("返回结果异常[{}]: {}", url, resBody));
            }
            return CollUtil.getFirst(JSONUtil.toBean(resBody, EmbyPlaybackOut.class).getMediaSources());
        } catch (Exception e) {
            log.warn("getPlayback 网络请求异常: ", e);
        }
        return null;
    }

    /**
     * 刷新媒体信息
     * <p>
     * 媒体信息有 但是有问题的话需要进行强制刷新
     *
     * @param mediaSourceId 媒体源id
     * @return {@link TmdbImageInfoOut }
     */
    public EmbyMediaSource refreshPlayback(String mediaSourceId) {
        String url = embyConfig.getHost() + StrUtil.format(embyConfig.getPlaybackUrl(), mediaSourceId);
        try (Response res = httpClient.send(Request.of(url).method(Method.POST)
                .form(MapUtil.<String, Object>builder("api_key", embyConfig.getApiKey())
                        .put("AutoOpenLiveStream", true).put("IsPlayback", true).map()))) {
            if (!res.isOk()) {
                throw new BaseException(StrUtil.format("返回码异常[{}]: {}", res.getStatus(), url));
            }
            String resBody = res.bodyStr();
            if (!JSONUtil.isTypeJSON(resBody)) {
                throw new BaseException(StrUtil.format("返回结果异常[{}]: {}", url, resBody));
            }
            return CollUtil.getFirst(JSONUtil.toBean(resBody, EmbyPlaybackOut.class).getMediaSources());
        } catch (Exception e) {
            log.warn("refreshPlayback 网络请求异常: ", e);
        }
        return null;
    }

    /**
     * 刷新媒体信息
     * <p>
     * 媒体信息有 但是有问题的话需要进行强制刷新
     *
     * @param mediaSourceId 媒体源id
     * @return {@link TmdbImageInfoOut }
     */
    public List<EmbyItem> getEpisodes(String mediaSourceId, String seasonId) {
        String url = embyConfig.getHost() + StrUtil.format(embyConfig.getEpisodesUrl(), mediaSourceId);
        try (Response res = httpClient.send(Request.of(url).method(Method.GET)
                .form(MapUtil.<String, Object>builder("api_key", embyConfig.getApiKey())
                        .put("fields", "Path,MediaSources,ProviderIds").put("seasonid", seasonId).map()))) {
            if (!res.isOk()) {
                throw new BaseException(StrUtil.format("返回码异常[{}]: {}", res.getStatus(), url));
            }
            String resBody = res.bodyStr();
            if (!JSONUtil.isTypeJSON(resBody)) {
                throw new BaseException(StrUtil.format("返回结果异常[{}]: {}", url, resBody));
            }
            return JSONUtil.toBean(resBody, EmbyItemsInfoOut.class).getItems();
        } catch (Exception e) {
            log.warn("getEpisodes 网络请求异常: ", e);
        }
        return null;
    }

    /**
     * 获取项目信息
     *
     * @param mediaSourceId 媒体源id
     * @return {@link TmdbImageInfoOut }
     */
    public EmbyItem getItemInfo(String mediaSourceId) {
        if (StrUtil.isBlank(mediaSourceId)) {
            return null;
        }
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
            EmbyItem embyItem = CollUtil.getFirst(JSONUtil.toBean(resBody, EmbyItemsInfoOut.class).getItems());
            return embyItem;
        } catch (Exception e) {
            log.warn("getItemInfo 网络请求异常: ", e);
        }
        return null;
    }

    /**
     * 获取项目信息
     *
     * @param mediaSourceId 媒体源id
     * @return {@link TmdbImageInfoOut }
     */
    public EmbyItem getItemInfoByCache(String mediaSourceId) {
        if (StrUtil.isBlank(mediaSourceId)) {
            return null;
        }
        String cacheKey = CacheUtil.buildThirdCacheKey("getItemInfo", mediaSourceId);
        EmbyItem result = redisClient.getBean(cacheKey);
        if (null != result) {
            return result;
        }
        result = getItemInfo(mediaSourceId);
        if (null != result && result.getSize() > 1024 * 1024L) {
            redisClient.setBean(cacheKey, result, 2 * 60);
        }
        return result;
    }


    public void initTmdbProvider(EmbyItem embyItem) {
        try {
            if (null == embyItem) {
                return;
            }
            Map<String, String> prividerMap = embyItem.getProviderIds();
            String embyType = embyItem.getType();
            if (MapUtil.isEmpty(prividerMap) ||
                    !StrUtil.equalsAnyIgnoreCase(embyType, 电影.getEmbyName(), 电视剧.getEmbyName())) {
                return;
            }

            String tmdbId = MapUtil.getStr(prividerMap, "Tmdb");
            if (StrUtil.isBlank(tmdbId)) {
                return;
            }
            TmdbProvider tmdbProvider = tmdbProviderDao.findByTmdb(tmdbId, embyType);
            if (null != tmdbProvider) {
                return;
            }
            String doubanId = MapUtil.getStr(prividerMap, "Douban");
            String imdbId = MapUtil.getStr(prividerMap, "Imdb");
            String tvdbId = MapUtil.getStr(prividerMap, "Tvdb");
            tmdbProvider = TmdbProvider.x().setTmdbId(tmdbId).setEmbyType(embyType)
                    .setDoubanId(doubanId).setImdbId(imdbId).setTvdbId(tvdbId);
            EmbyMediaType embyMediaType = EmbyMediaType.fromEmby(embyType);

            // 获取tmdb信息
            String tmdbInfo = tmdbProxy.getInfoById(embyMediaType, tmdbId);
            if (StrUtil.isNotBlank(tmdbInfo)) {
                tmdbProvider.setTmdbInfo(tmdbInfo);
            }
            tmdbProviderDao.insertOrUpdate(tmdbProvider);
        } catch (Exception e) {
            log.warn("[itemId:{}]初始化构建tmdb-douban失败: ", embyItem.getItemId(), e);
        }
    }


    public synchronized void expertTmdbProvider(EmbyItem embyItem) {
        try {
            if (null == embyItem) {
                return;
            }
            Map<String, String> prividerMap = embyItem.getProviderIds();
            String embyType = embyItem.getType();
            if (MapUtil.isEmpty(prividerMap) ||
                    !StrUtil.equalsAnyIgnoreCase(embyType, 电影.getEmbyName(), 电视剧.getEmbyName())) {
                return;
            }

            String tmdbId = MapUtil.getStr(prividerMap, "Tmdb");
            if (StrUtil.isBlank(tmdbId)) {
                return;
            }
            TmdbProvider tmdbProvider = tmdbProviderDao.findByTmdb(tmdbId, embyType);
            if (null != tmdbProvider) {
                return;
            }
            log.info("hook构建tmdb-douban: itemId: {},tmdbId: {}", embyItem.getItemId(), tmdbId);
            String doubanId = MapUtil.getStr(prividerMap, "Douban");
            String imdbId = MapUtil.getStr(prividerMap, "Imdb");
            String tvdbId = MapUtil.getStr(prividerMap, "Tvdb");
            tmdbProvider = TmdbProvider.x().setTmdbId(tmdbId).setEmbyType(embyType)
                    .setDoubanId(doubanId).setImdbId(imdbId).setTvdbId(tvdbId);
            EmbyMediaType embyMediaType = EmbyMediaType.fromEmby(embyType);

            // 获取tmdb信息
            String tmdbInfo = tmdbProxy.getInfoById(embyMediaType, tmdbId);
            if (StrUtil.isNotBlank(tmdbInfo)) {
                tmdbProvider.setTmdbInfo(tmdbInfo);
            }
            // 获取豆瓣信息
            if (StrUtil.isBlank(doubanId) && StrUtil.isNotBlank(imdbId)) {
                // 如果没有豆瓣id 但是有imdbid 则尝试通过imdbid获取豆瓣id
                doubanId = doubanProxy.getDoubanIdByImdbId(embyMediaType, imdbId);
                if (StrUtil.isNotBlank(doubanId)) {
                    tmdbProvider.setDoubanId(doubanId);
                }
            }
            if (StrUtil.isNotBlank(doubanId)) {
                String doubanInfo = doubanProxy.getInfoById(embyMediaType, doubanId);
                ThreadUtil.safeSleep(5000);
                if (StrUtil.isNotBlank(doubanInfo)) {
                    tmdbProvider.setDoubanInfo(doubanInfo);
                    tmdbProvider.setDoubanRate(JSONUtil.parseObj(doubanInfo)
                            .getJSONObject("rating").getBigDecimal("value"));
                    tmdbProvider.setRateUpdateTime(new DateTime());
                }
            }
            tmdbProviderDao.insertOrUpdate(tmdbProvider);
        } catch (Exception e) {
            log.warn("[itemId:{}]构建tmdb-douban失败: ", embyItem.getItemId(), e);
        }
    }

    /**
     * 获取项目信息列表
     *
     * @param items items
     * @return {@link TmdbImageInfoOut }
     */
    public EmbyItemsInfoOut getItemInfos(List<String> items) {
        String url = embyConfig.getHost() + embyConfig.getItemInfoUrl();
        try (Response res = httpClient.send(Request.of(url).method(Method.GET)
                .form(MapUtil.<String, Object>builder("Fields", "Path,MediaSources,ProviderIds")
                        .put("Ids", StrUtil.join(COMMA, items))
                        .put("api_key", embyConfig.getApiKey()).map()))) {
            if (!res.isOk()) {
                throw new BaseException(StrUtil.format("返回码异常[{}]: {}", res.getStatus(), url));
            }
            String resBody = res.bodyStr();
            if (!JSONUtil.isTypeJSON(resBody)) {
                throw new BaseException(StrUtil.format("返回结果异常[{}]: {}", url, resBody));
            }
            return JSONUtil.toBean(resBody, EmbyItemsInfoOut.class);
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
                // throw new BaseException(StrUtil.format("返回码异常[{}]: {}", res.getStatus(), url));
                return EmbyRemoteImageOut.Img.builder().url("undefined").build();
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

    /**
     * 原始出参转换为 项目出参
     *
     * @param res     事件
     * @param request 要求
     * @return {@link EmbyCachedResp }
     */
    @SneakyThrows
    public EmbyCachedResp transferResp(Response res, EmbyContentCacheReqWrapper request) {
        EmbyCachedResp embyCachedResp = new EmbyCachedResp();
        embyCachedResp.setStatusCode(res.getStatus());
        if (!res.isOk()) {
            return embyCachedResp;
        }
        res.headers().forEach((k, v) -> {
            if (EmbyProxyUtil.isNotAllowedHeader(k)) {
                return;
            }
            embyCachedResp.getHeaders().put(k, StrUtil.join(COMMA, v));
        });
        if (res.getStatus() == CODE_204) {
            return embyCachedResp;
        }

        ResponseBody body = res.body();
        if (null == body) {
            return embyCachedResp;
        }

        if (ServletUtil.isGetMethod(request) && StrUtil.containsIgnoreCase(
                embyCachedResp.getHeaders().get("Content-Type"), "application/json")) {
            // embyCachedResp.getHeaders().remove("Content-Length");
            String content = "";
            byte[] bodyBytes = body.getBytes();
            /*if (StrUtil.equalsIgnoreCase(embyCachedResp.getHeaders().get("Content-Encoding"), "br")) {
                String bodyStr = new String(CompressUtil.decode(bodyBytes));
                content = changeRespBody(request, bodyStr);
                embyCachedResp.setContent(content.getBytes());
                embyCachedResp.getHeaders().remove("Content-Encoding");
                log.info("br解码: {}", bodyStr);
            } else if (StrUtil.containsIgnoreCase(embyCachedResp.getHeaders().get("Content-Encoding"), "deflate")) {
                String bodyStr = new String(ZipUtil.unZlib(bodyBytes));
                content = changeRespBody(request, bodyStr);
                embyCachedResp.setContent(ZipUtil.zlib(content.getBytes(), 5));
                log.info("deflate解码: {}", bodyStr);
            } else if (StrUtil.containsIgnoreCase(embyCachedResp.getHeaders().get("Content-Encoding"), "gzip")) {
                String bodyStr = new String(ZipUtil.unGzip(bodyBytes));
                content = changeRespBody(request, bodyStr);
                embyCachedResp.setContent(ZipUtil.gzip(content.getBytes()));
                log.info("gzip解码: {}", bodyStr);
            } else {*/
            String bodyStr = new String(bodyBytes);
            if (!JSONUtil.isTypeJSON(bodyStr)) {
                log.warn("非json: {}", bodyStr);
            }
            content = changeRespBody(request, bodyStr);
            embyCachedResp.setContent(content.getBytes());
            // }

            // 禁用缓存（防止客户端缓存未替换的内容）
            // embyCachedResp.getHeaders().put("Cache-Control", "no-cache, no-store, must-revalidate");
            // embyCachedResp.getHeaders().put("Pragma", "no-cache");
            // embyCachedResp.getHeaders().put("Expires", "0");
            // embyCachedResp.getHeaders().put("Content-Length", content.getBytes().length + "");
        } else {
            embyCachedResp.setContent(body.getBytes());
        }

        return embyCachedResp;
    }

    private String changeRespBody(EmbyContentCacheReqWrapper request, String bodyStr) {
        refreshItem(request, bodyStr);
        bodyStr = searchItem(request, bodyStr);
        bodyStr = reBuildView(request, bodyStr);
        bodyStr = reBuildLatest(request, bodyStr);
        return StrUtil.replaceIgnoreCase(bodyStr, "micu", "REDMT");
    }


    /**
     * 返回结果个性化排序
     *
     * @param request 要求
     * @param bodyStr 身体str
     */
    private static String searchItem(EmbyContentCacheReqWrapper request, String bodyStr) {
        if (!ReUtil.isItemsUrl(request.getRequestURI().toLowerCase()) ||
                !request.getCachedParam().containsKey("SearchTerm")) {
            return bodyStr;
        }
        EmbyItemsInfoOut itemInfo = JSONUtil.toBean(bodyStr, EmbyItemsInfoOut.class);
        if (CollUtil.isEmpty(itemInfo.getItems())) {
            return bodyStr;
        }
        List<EmbyItem> items = SortUtil.searchSortItem(itemInfo.getItems(),
                request.getCachedParam().get("SearchTerm").toString());
        itemInfo.setItems(items);
        return JSONUtil.toJsonStr(itemInfo);
    }

    /**
     * 重建虚拟视图
     *
     * @param request 要求
     * @param bodyStr 身体str
     * @return {@link String }
     */
    private static String reBuildView(EmbyContentCacheReqWrapper request, String bodyStr) {
        if (request.getToolkitView() == null) {
            return bodyStr;
        }
        JSONObject viewJn = JSONUtil.parseObj(bodyStr);
        JSONArray items = viewJn.getJSONArray("Items");
        items.addAll(0, request.getToolkitView());
        items.removeIf(item -> {
            JSONObject jn = item.asJSONObject();
            return StrUtil.equalsAny(jn.getStr("Name"), "🎬 华语电影", "🎬 外语电影", "🐦 动画电影",
                    "🐧 动漫", "🐧 国漫", "📺 国产剧", "📺 欧美剧", "📺 日韩剧", "🎭 综艺", "🦉 记录电影", "🦉 纪录片");
        });
        return viewJn.toString();
    }

    /**
     * 重建虚拟视图
     *
     * @param request 要求
     * @param bodyStr 身体str
     * @return {@link String }
     */
    private static String reBuildLatest(EmbyContentCacheReqWrapper request, String bodyStr) {
        if (!StrUtil.containsIgnoreCase(request.getRequestURI(), "Items/Latest")) {
            return bodyStr;
        }
        if (!JSONUtil.isTypeJSONObject(bodyStr)) {
            return bodyStr;
        }
        return JSONUtil.parseObj(bodyStr).getStr("Items");
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
        /*ThreadUtil.execVirtual(() -> {
            EmbyItem item = JSONUtil.toBean(bodyStr, EmbyItem.class);
            if (item.getIsFolder() || !StrUtil.equalsAnyIgnoreCase(item.getType(), 电影.getEmbyName(),
                    电视剧_集.getEmbyName(), 电视剧_季.getEmbyName(), 电视剧.getEmbyName())) {
                return;
            }
            if (!StrUtil.containsAny(item.getUniqueKey(), "tmdb", "tt", "zh-CN-cf")
                    || StrUtil.isNotBlank(item.getImageTags().getPrimary())
                    || StrUtil.equalsIgnoreCase(item.getContainer(), "strm")
                    || item.getSize() < 1024 * 1024L) {

                return;
            }
            String lockKey = LockUtil.buildRefreshTmdbLock(item.getItemId());
            if (redisClient.get(lockKey) != null) {
                return;
            }
            try {
                refresh(item.getItemId());
            } finally {
                redisClient.set(lockKey, "1", 2 * 60 * 60);
            }
        });*/
        ThreadUtil.execVirtual(() -> {
            EmbyItem item = JSONUtil.toBean(bodyStr, EmbyItem.class);
            if (item.getIsFolder() || !StrUtil.equalsAnyIgnoreCase(item.getType(), 电影.getEmbyName(),
                    电视剧_集.getEmbyName())) {
                return;
            }
            String lockKey = LockUtil.buildRefreshMediaLock(item.getItemId());
            if (!redisClient.setnx(lockKey, 1, 5 * 60)) {
                return;
            }
            try {
                getPlayback(item.getItemId());
            } finally {
                redisClient.del(lockKey);
            }
        });
    }

    /**
     * 115离线至113
     *
     * @param strm 扫描隧道显微镜
     */
    public void trans115To123(String strm) {
        if (StrUtil.isBlank(strm)) {
            return;
        }
        UrlBuilder urlBuilder = UrlBuilder.of(UrlDecoder.decode(strm));
        UrlPath urlPath = urlBuilder.getPath();
        List<CharSequence> allPath = urlPath.getSegments();
        List<CharSequence> path115Seq = CollUtil.sub(allPath, 2, -1);
        List<CharSequence> path113Seq = CollUtil.sub(allPath, 2, -2);
        String path115 = "/" + StrUtil.join("/", path115Seq);
        String path123 = "/" + StrUtil.join("/", path113Seq);

        String url = embyConfig.getEmbyNginxHost() + "/api/v1/transfer_115_to_123";
        try (Response res = httpClient.send(Request.of(url).method(Method.POST)
                .body(JSONUtil.toJsonStr(MapUtil.<String, Object>builder("cookie_name_115", "115生活ios端")
                        .put("path_in_115", path115)
                        .put("cookie_name_123", "123cookie").put("path_in_123", path123)
                        .put("is_clear_records", "0").map())))) {
            if (!res.isOk()) {
                throw new BaseException(StrUtil.format("返回码异常[{}]: {}", res.getStatus(), url));
            }
            String resBody = res.bodyStr();
            if (!JSONUtil.isTypeJSON(resBody)) {
                throw new BaseException(StrUtil.format("返回结果异常[{}]: {}", url, resBody));
            }
        } catch (Exception e) {
            log.warn("trans115To123 网络请求异常: ", e);
        }
    }

    /*public static void main(String[] args) {

        String strm = "http://192.168.1.249:5244/d/new115/emby2/电影/外语电影/侏罗纪世界：重生 (2025) [tmdbid=1234821]/侏罗纪世界：重生 (2025).2160p.DoVi.HDR.H.265.DDP Atmos 5.1.mkv" +
                "";
        UrlBuilder urlBuilder = UrlBuilder.of(UrlDecoder.decode(strm));
        UrlPath urlPath = urlBuilder.getPath();
        List<CharSequence> allPath = urlPath.getSegments();
        List<CharSequence> path115Seq = CollUtil.sub(allPath, 2, -1);
        List<CharSequence> path113Seq = CollUtil.sub(allPath, 2, -2);
        String path115 = "/" + StrUtil.join("/", path115Seq);
        String path123 = "/" + StrUtil.join("/", path113Seq);
        String url = "http://192.168.1.249:9527" + "/api/v1/transfer_115_to_123";
        try (Response res = HttpUtil.send(Request.of(url).method(Method.POST)
                .body(JSONUtil.toJsonStr(MapUtil.<String, Object>builder("cookie_name_115", "115生活ios端")
                        .put("path_in_115", path115)
                        .put("cookie_name_123", "123cookie")
                        .put("path_in_123", path123)
                        .put("is_clear_records", "0").map())))) {

            if (!res.isOk()) {
                throw new BaseException(StrUtil.format("返回码异常[{}]: {}", res, url));
            }
            String resBody = res.bodyStr();
            if (!JSONUtil.isTypeJSON(resBody)) {
                throw new BaseException(StrUtil.format("返回结果异常[{}]: {}", url, resBody));
            }
        } catch (Exception e) {
            log.warn("trans115To123 网络请求异常: ", e);
        }
    }*/
}