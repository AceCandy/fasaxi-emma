package cn.acecandy.fasaxi.emma.sao.proxy;

import cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType;
import cn.acecandy.fasaxi.emma.common.enums.EmbyPicType;
import cn.acecandy.fasaxi.emma.common.enums.StrmPathPrefix;
import cn.acecandy.fasaxi.emma.common.ex.BaseException;
import cn.acecandy.fasaxi.emma.common.resp.EmbyCachedResp;
import cn.acecandy.fasaxi.emma.config.EmbyConfig;
import cn.acecandy.fasaxi.emma.config.EmbyContentCacheReqWrapper;
import cn.acecandy.fasaxi.emma.dao.embyboss.entity.TmdbProvider;
import cn.acecandy.fasaxi.emma.dao.embyboss.entity.VideoPathRelation;
import cn.acecandy.fasaxi.emma.dao.embyboss.service.TmdbProviderDao;
import cn.acecandy.fasaxi.emma.dao.embyboss.service.VideoPathRelationDao;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.client.RedisLockClient;
import cn.acecandy.fasaxi.emma.sao.out.EmbyItem;
import cn.acecandy.fasaxi.emma.sao.out.EmbyItemsInfoOut;
import cn.acecandy.fasaxi.emma.sao.out.EmbyMediaSource;
import cn.acecandy.fasaxi.emma.sao.out.EmbyPlaybackOut;
import cn.acecandy.fasaxi.emma.sao.out.EmbyRemoteImageOut;
import cn.acecandy.fasaxi.emma.sao.out.EmbyViewOut;
import cn.acecandy.fasaxi.emma.sao.out.TmdbImageInfoOut;
import cn.acecandy.fasaxi.emma.utils.CacheUtil;
import cn.acecandy.fasaxi.emma.utils.CloudUtil;
import cn.acecandy.fasaxi.emma.utils.EmbyProxyUtil;
import cn.acecandy.fasaxi.emma.utils.ReUtil;
import cn.acecandy.fasaxi.emma.utils.SortUtil;
import cn.acecandy.fasaxi.emma.utils.ThreadUtil;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.core.date.DateFormatPool;
import cn.hutool.v7.core.date.DateTime;
import cn.hutool.v7.core.date.DateUtil;
import cn.hutool.v7.core.exception.ExceptionUtil;
import cn.hutool.v7.core.io.file.FileUtil;
import cn.hutool.v7.core.lang.Console;
import cn.hutool.v7.core.lang.mutable.MutableTriple;
import cn.hutool.v7.core.map.MapUtil;
import cn.hutool.v7.core.math.NumberUtil;
import cn.hutool.v7.core.net.url.UrlBuilder;
import cn.hutool.v7.core.net.url.UrlDecoder;
import cn.hutool.v7.core.net.url.UrlPath;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.http.HttpUtil;
import cn.hutool.v7.http.client.Request;
import cn.hutool.v7.http.client.Response;
import cn.hutool.v7.http.client.body.ResponseBody;
import cn.hutool.v7.http.client.engine.ClientEngine;
import cn.hutool.v7.http.meta.Method;
import cn.hutool.v7.http.server.servlet.ServletUtil;
import cn.hutool.v7.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_204;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_302;
import static cn.acecandy.fasaxi.emma.common.enums.CloudStorageType.R_115;
import static cn.acecandy.fasaxi.emma.common.enums.CloudStorageType.R_123;
import static cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType.电影;
import static cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType.电视剧;
import static cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType.电视剧_集;
import static cn.acecandy.fasaxi.emma.sao.client.RedisLockClient.buildItemsLock;
import static cn.acecandy.fasaxi.emma.sao.client.RedisLockClient.buildRefreshMediaLock;
import static cn.hutool.v7.core.text.StrPool.COMMA;

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
    private RedisLockClient redisLockClient;

    @Resource
    private TmdbProviderDao tmdbProviderDao;

    @Resource
    private DoubanProxy doubanProxy;

    @Resource
    private TmdbProxy tmdbProxy;

    @Resource
    private VideoPathRelationDao videoPathRelationDao;

    @Resource
    private CloudUtil cloudUtil;

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
     * 在合集中搜索
     *
     * @param collectionId 合集ID
     * @return {@link List<EmbyItem> }
     */
    public List<EmbyItem> searchItemsByCollections(String searchTerm, String itemTypes, Integer collectionId) {
        if (StrUtil.isBlank(searchTerm) || null == collectionId) {
            return ListUtil.ofLinked();
        }
        String url = embyConfig.getHost() + embyConfig.getItemInfoUrl();
        try (Response res = httpClient.send(Request.of(url).method(Method.GET)
                .form(Map.of("api_key", embyConfig.getApiKey(),
                        "Recursive", true,
                        "IncludeItemTypes", itemTypes,
                        "SearchTerm", searchTerm,
                        "Limit", 3,
                        "ParentId", collectionId)))) {
            if (!res.isOk()) {
                throw new BaseException(StrUtil.format("返回码异常[{}]: {}", res.getStatus(), url));
            }
            String resBody = res.bodyStr();
            if (!JSONUtil.isTypeJSON(resBody)) {
                throw new BaseException(StrUtil.format("返回结果异常[{}]: {}", url, resBody));
            }
            return JSONUtil.toBean(resBody, EmbyItemsInfoOut.class).getItems();
        } catch (Exception e) {
            log.warn("getItemsByCollections 网络请求异常: ", e);
        }
        return null;
    }

    /**
     * 获取合集items
     *
     * @param collectionId 合集ID
     * @return {@link List<EmbyItem> }
     */
    public List<EmbyItem> getItemsByCollections(String collectionId) {
        String url = embyConfig.getHost() + embyConfig.getItemInfoUrl();
        try (Response res = httpClient.send(Request.of(url).method(Method.GET)
                .form(Map.of("api_key", embyConfig.getApiKey(),
                        "ParentId", collectionId)))) {
            if (!res.isOk()) {
                throw new BaseException(StrUtil.format("返回码异常[{}]: {}", res.getStatus(), url));
            }
            String resBody = res.bodyStr();
            if (!JSONUtil.isTypeJSON(resBody)) {
                throw new BaseException(StrUtil.format("返回结果异常[{}]: {}", url, resBody));
            }
            return JSONUtil.toBean(resBody, EmbyItemsInfoOut.class).getItems();
        } catch (Exception e) {
            log.warn("getItemsByCollections 网络请求异常: ", e);
        }
        return null;
    }

    /**
     * 创建合集
     *
     */
    public String createCollections(String name, List<String> itemIds) {
        if (CollUtil.isEmpty(itemIds)) {
            log.info("createCollections 空项目ID, 合集名称: {}", name);
            return null;
        }
        String ids = StrUtil.join(COMMA, itemIds);
        String url = embyConfig.getHost() + StrUtil.format("/Collections?Name={}&Ids={}&api_key={}",
                name, ids, embyConfig.getApiKey());
        try (Response res = httpClient.send(Request.of(url).method(Method.POST))) {
            if (!res.isOk()) {
                throw new BaseException(StrUtil.format("返回码异常[{}]: {}", res.getStatus(), url));
            }
            String resBody = res.bodyStr();
            if (!JSONUtil.isTypeJSON(resBody)) {
                throw new BaseException(StrUtil.format("返回结果异常[{}]: {}", url, resBody));
            }
            return JSONUtil.parseObj(resBody).getStr("Id");
        } catch (Exception e) {
            log.warn("createCollections 网络请求异常: ", e);
        }
        return null;
    }

    /**
     * 往合集中新增items
     *
     * @param collectionId 合集ID
     * @param itemIds      项目ID
     */
    public void addItemsByCollections(String collectionId, List<String> itemIds) {
        if (CollUtil.isEmpty(itemIds)) {
            log.info("addItemsByCollections 空项目ID, 合集ID: {}", collectionId);
            return;
        }
        CollUtil.partition(itemIds, 200).forEach(i -> {
            String itemIdsStr = StrUtil.join(COMMA, i);
            String url = embyConfig.getHost() + StrUtil.format(embyConfig.getCollectionAddUrl(),
                    collectionId, embyConfig.getApiKey(), itemIdsStr);
            try (Response res = httpClient.send(Request.of(url).method(Method.POST))) {
                if (!res.isOk()) {
                    throw new BaseException(StrUtil.format("返回码异常[{}]: {}", res.getStatus(), url));
                }
            } catch (Exception e) {
                log.warn("addItemsByCollections 网络请求异常: ", e);
            }
        });
    }

    /**
     * 往合集中删除items
     *
     * @param collectionId 合集ID
     * @param itemIds      项目ID
     */
    public void delItemsByCollections(String collectionId, List<String> itemIds) {
        if (CollUtil.isEmpty(itemIds)) {
            log.info("delItemsByCollections 空项目ID, 合集ID: {}", collectionId);
            return;
        }
        CollUtil.partition(itemIds, 200).forEach(i -> {
            String itemIdsStr = StrUtil.join(COMMA, i);
            String url = embyConfig.getHost() + StrUtil.format(embyConfig.getCollectionDelUrl(),
                    collectionId, embyConfig.getApiKey(), itemIdsStr);
            try (Response res = httpClient.send(Request.of(url).method(Method.POST))) {
                if (!res.isOk()) {
                    throw new BaseException(StrUtil.format("返回码异常[{}]: {}", res.getStatus(), url));
                }
            } catch (Exception e) {
                log.warn("delItemsByCollections 网络请求异常: ", e);
            }
        });
    }

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
    public EmbyItemsInfoOut getUserItems(String userId, Map<String, Object> cachedParam) {
        String url = embyConfig.getHost() + StrUtil.format(embyConfig.getUserItemUrl(), userId);
        cachedParam.put("api_key", embyConfig.getApiKey());
        cachedParam.remove("Recursive");
        cachedParam.remove("MediaTypes");
        cachedParam.put("IncludeItemTypes", "Series,Movie");
        try (Response res = httpClient.send(Request.of(url).method(Method.GET).form(cachedParam))) {
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
                .put("Recursive", true).put("Filters", "IsUnplayed")
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
     * 获取整库的媒体项
     *
     * @param parentId 库id
     * @return {@link List<EmbyItem> }
     */
    public List<EmbyItem> getItemsByParentIdOnLock(String parentId) {
        String lockKey = buildItemsLock(parentId);
        if (!redisLockClient.lock(lockKey, 600)) {
            return ListUtil.of();
        }
        String url = embyConfig.getHost() + embyConfig.getItemInfoUrl();

        int start = 0;
        int batchSize = 5000;

        List<EmbyItem> items = ListUtil.of();
        try {
            while (true) {
                try (Response res = httpClient.send(Request.of(url).method(Method.GET).form(Map.of(
                        "api_key", embyConfig.getApiKey(), "Recursive", true,
                        "IncludeItemTypes", "Movie,Series",
                        "ParentId", parentId, "StartIndex", start, "Limit", batchSize)))) {
                    if (!res.isOk()) {
                        throw new BaseException(StrUtil.format("返回码异常[{}]: {}", res.getStatus(), url));
                    }
                    String resBody = res.bodyStr();
                    if (!JSONUtil.isTypeJSON(resBody)) {
                        throw new BaseException(StrUtil.format("返回结果异常[{}]: {}", url, resBody));
                    }
                    EmbyItemsInfoOut out = JSONUtil.toBean(resBody, EmbyItemsInfoOut.class);
                    List<EmbyItem> itemList = out.getItems();
                    items.addAll(itemList);
                    if (CollUtil.isEmpty(itemList) || CollUtil.size(itemList) < batchSize) {
                        break;
                    }
                    start += CollUtil.size(itemList);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (Exception e) {
            log.warn("getItemsByParentIdOnLock 网络请求异常: ", e);
        } finally {
            redisLockClient.unlock(lockKey);
        }
        return items;
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
                        .put("fields", "Path,MediaSources,ProviderIds").put("SeasonId", seasonId).map()))) {
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

    public List<EmbyItem> getEpisodesUserByCache(String mediaSourceIds, String seasonId, String userId) {
        if (StrUtil.isBlank(mediaSourceIds) || StrUtil.isBlank(seasonId) || StrUtil.isBlank(userId)) {
            return null;
        }
        String cacheKey = CacheUtil.buildThirdCacheKey("getEpisodes",
                mediaSourceIds + "_" + seasonId);
        List<EmbyItem> result = redisClient.getBean(cacheKey);
        if (CollUtil.isNotEmpty(result)) {
            return result;
        }
        result = getEpisodesUser(mediaSourceIds, seasonId, userId);
        if (CollUtil.isNotEmpty(result)) {
            redisClient.setBean(cacheKey, result, 60 * 20);
        }
        return result;
    }

    /**
     * 刷新媒体信息
     * <p>
     * 媒体信息有 但是有问题的话需要进行强制刷新
     *
     * @param mediaSourceId 媒体源id
     * @return {@link TmdbImageInfoOut }
     */
    public List<EmbyItem> getEpisodesUser(String mediaSourceId, String seasonId, String userId) {
        if (StrUtil.isBlank(mediaSourceId) || StrUtil.isBlank(seasonId) || StrUtil.isBlank(userId)) {
            return null;
        }
        String url = embyConfig.getHost() + StrUtil.format(embyConfig.getEpisodesUrl(), mediaSourceId);
        try (Response res = httpClient.send(Request.of(url).method(Method.GET)
                .form(MapUtil.<String, Object>builder("api_key", embyConfig.getApiKey())
                        .put("UserId", userId).put("SeasonId", seasonId).map()))) {
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
     * @param mediaSourceIds 媒体源id
     * @return {@link TmdbImageInfoOut }
     */
    public EmbyItem getUserItemMedia(String userId, String mediaSourceIds) {
        if (StrUtil.isBlank(userId) || StrUtil.isBlank(mediaSourceIds)) {
            return null;
        }
        String url = embyConfig.getHost() + StrUtil.format(embyConfig.getUserItemMediaUrl(), userId, mediaSourceIds);
        try (Response res = httpClient.send(Request.of(url).method(Method.GET)
                .form(MapUtil.<String, Object>builder("api_key", embyConfig.getApiKey()).map()))) {
            if (!res.isOk()) {
                throw new BaseException(StrUtil.format("返回码异常[{}]: {}", res.getStatus(), url));
            }
            String resBody = res.bodyStr();
            if (!JSONUtil.isTypeJSON(resBody)) {
                throw new BaseException(StrUtil.format("返回结果异常[{}]: {}", url, resBody));
            }
            return JSONUtil.toBean(resBody, EmbyItem.class);
        } catch (Exception e) {
            log.warn("getUserItemMedia 网络请求异常: ", e);
        }
        return null;
    }

    /**
     * 获取项目信息
     *
     * @param mediaSourceIds 媒体源id
     * @return {@link TmdbImageInfoOut }
     */
    public List<EmbyItem> getItemInfo(String mediaSourceIds) {
        if (StrUtil.isBlank(mediaSourceIds)) {
            return null;
        }
        String url = embyConfig.getHost() + embyConfig.getItemInfoUrl();
        try (Response res = httpClient.send(Request.of(url).method(Method.GET)
                .form(MapUtil.<String, Object>builder("Fields", "Path,MediaSources,ProviderIds,DateModified")
                        .put("Ids", mediaSourceIds)
                        .put("api_key", embyConfig.getApiKey()).map()))) {
            if (!res.isOk()) {
                throw new BaseException(StrUtil.format("返回码异常[{}]: {}", res.getStatus(), url));
            }
            String resBody = res.bodyStr();
            if (!JSONUtil.isTypeJSON(resBody)) {
                throw new BaseException(StrUtil.format("返回结果异常[{}]: {}", url, resBody));
            }
            return JSONUtil.toBean(resBody, EmbyItemsInfoOut.class).getItems();
        } catch (Exception e) {
            log.warn("getItemInfo 网络请求异常: ", e);
        }
        return null;
    }

    /**
     * 获取项目信息
     *
     * @param mediaSourceIds 媒体源id
     * @return {@link TmdbImageInfoOut }
     */
    public List<EmbyItem> getItemInfoByCache(String mediaSourceIds) {
        if (StrUtil.isBlank(mediaSourceIds)) {
            return null;
        }
        String cacheKey = CacheUtil.buildThirdCacheKey("getItemInfo", mediaSourceIds);
        List<EmbyItem> result = redisClient.getBean(cacheKey);
        if (CollUtil.isNotEmpty(result)) {
            return result;
        }
        result = getItemInfo(mediaSourceIds);
        if (CollUtil.isNotEmpty(result)) {
            redisClient.setBean(cacheKey, result, 60);
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
            String bodyStr = new String(bodyBytes);
            if (!JSONUtil.isTypeJSON(bodyStr)) {
                log.warn("非json: {}", bodyStr);
            }
            content = changeRespBody(request, bodyStr);
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
     * 刷新项目
     *
     * @param request 要求
     * @param bodyStr 身体str
     */
    private void refreshItem(EmbyContentCacheReqWrapper request, String bodyStr) {
        List<String> reStr = ReUtil.isItemUrl(request.getRequestURI().toLowerCase());
        if (CollUtil.isEmpty(reStr)) {
            return;
        }
        EmbyItem item = JSONUtil.toBean(bodyStr, EmbyItem.class);
        itemAndNextHandle(request.getUserId(), request.getUa(), request.getDeviceId(), item);
    }

    public void itemAndNextHandle(String userId, String ua, String deviceId, EmbyItem item) {
        ThreadUtil.execVirtual(() -> {
            currentItemHandle(ua, deviceId, item);
        });
        nextHandle(userId, ua, deviceId, item);
    }

    public void nextHandle(String userId, String ua, String deviceId, EmbyItem item) {
        if (StrUtil.isBlank(userId)) {
            userId = "9f711eef323044ed8fbc6e5531de0431";
        }
        String finalUserId = userId;
        ThreadUtil.execVirtual(() -> {

            if (!StrUtil.equals(电视剧_集.getEmbyName(), item.getType())) {
                return;
            }
            String itemId = item.getItemId();
            List<EmbyItem> seasonItem = getEpisodesUserByCache(itemId, item.getSeasonId(), finalUserId);
            int index = -1;
            for (int i = 0; i < seasonItem.size(); i++) {
                if (seasonItem.get(i).getItemId().equals(itemId)) {
                    index = i;
                    break;
                }
            }
            if (index == -1 || index == seasonItem.size() - 1) {
                // 没有找到符合条件或者已经最后一集了
                return;
            }
            EmbyItem nexItem = getUserItemMedia(finalUserId,
                    seasonItem.get(index + 1).getItemId());
            currentItemHandle(ua, deviceId, nexItem);
        });
    }

    private void currentItemHandle(String ua, String deviceId, EmbyItem item) {
        if (item.getIsFolder() || !StrUtil.equalsAnyIgnoreCase(item.getType(), 电影.getEmbyName(),
                电视剧_集.getEmbyName())) {
            return;
        }
        String lockKey = buildRefreshMediaLock(item.getItemId());
        if (!redisLockClient.lock(lockKey, 120)) {
            return;
        }
        try {
            asyncUpdateVideoPathRelation(item, ua, deviceId);
            getPlayback(item.getItemId());
        } finally {
            redisLockClient.unlock(lockKey);
        }
    }

    /**
     * 异步更新视频路径关系
     *
     * @param item 物品信息
     */
    public void asyncUpdateVideoPathRelation(EmbyItem item, String ua, String deviceId) {
        try {
            List<EmbyMediaSource> mediaSources = item.getMediaSources();
            String mediaSourceIds = mediaSources.stream().map(EmbyMediaSource::getItemId)
                    .collect(Collectors.joining(","));
            List<EmbyItem> items = getItemInfoByCache(mediaSourceIds);
            items.forEach(itemInfo -> {
                Integer itemId = NumberUtil.parseInt(itemInfo.getItemId());
                try {
                    String itemPath = itemInfo.getPath();
                    Date nowStrmTime = FileUtil.lastModifiedTime(itemPath);
                    if (null == nowStrmTime) {
                        return;
                    }
                    EmbyMediaType itemType = EmbyMediaType.fromEmby(itemInfo.getType());

                    VideoPathRelation videoPathRelation = videoPathRelationDao.findById(itemId);

                    if (null != videoPathRelation && DateUtil.compare(nowStrmTime,
                            videoPathRelation.getStrmTime(), DateFormatPool.PURE_DATETIME_PATTERN) == 0) {
                        return;
                    }
                    if (null == videoPathRelation) {
                        String itemName = itemType == 电视剧_集 ? StrUtil.format("{}/{}/{}",
                                itemInfo.getSeriesName(), itemInfo.getSeasonName(), itemInfo.getName())
                                : itemInfo.getName();
                        videoPathRelation = VideoPathRelation.x().setItemName(itemName).setItemType(itemType.getEmbyName());
                    } else {
                        videoPathRelation = VideoPathRelation.x();
                    }

                    String realPath = CollUtil.getFirst(itemInfo.getMediaSources()).getPath();
                    // 只处理网络路径开头的标准化 本地格式保留
                    MutableTriple<String, StrmPathPrefix, String> pathSplit = StrmPathPrefix.split(realPath);
                    String strmType = pathSplit.getMiddle().getType();
                    String path115 = "", path123 = "";
                    if (StrUtil.equalsIgnoreCase(strmType, "123")) {
                        path123 = realPath;
                    } else if (StrUtil.equalsIgnoreCase(strmType, "115")) {
                        path115 = realPath;
                    }
                    videoPathRelation.setItemId(itemId).setBakStatus(0)
                            .setStrmTime(nowStrmTime).setEmbyTime(itemInfo.getDateModified())
                            .setStrmPath(itemPath).setRealPath(realPath).setStrmType(strmType)
                            .setPathPrefix(pathSplit.getMiddle().getValue()).setPurePath(pathSplit.getRight())
                            .setPath115(path115).setPath123(path123);
                    videoPathRelationDao.insertOrUpdate(videoPathRelation);
                } finally {
                    VideoPathRelation videoPathRelation = videoPathRelationDao.findById(itemId);
                    if (null != videoPathRelation) {
                        String path115 = videoPathRelation.getPath115();
                        if (StrUtil.isNotBlank(path115)) {
                            cloudUtil.reqAndCacheOpenList302Url(R_115, path115,
                                    ua, String.valueOf(itemId), deviceId);
                        }
                        String path123 = videoPathRelation.getPath123();
                        if (StrUtil.isNotBlank(path123)) {
                            cloudUtil.reqAndCacheOpenList302Url(R_123, path123,
                                    ua, String.valueOf(itemId), deviceId);
                        }
                    }
                }
            });
        } catch (Exception e) {
            log.error("[视频路径关系] 更新失败", e);
        }
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

    static void main() {
        String itemPath = "/Users/mac/Library/CloudStorage/OneDrive-个人/workspace/github/java/fasaxi-emma/src/main/java/cn/acecandy/fasaxi/emma/sao/proxy/EmbyProxy.java";
        DateTime nowStrmTime = DateUtil.date(FileUtil.lastModifiedTime(itemPath));
        VideoPathRelation videoPathRelation = VideoPathRelation.x().setStrmTime(FileUtil.lastModifiedTime(itemPath));

        Console.log(DateUtil.compare(nowStrmTime,
                videoPathRelation.getStrmTime(), DateFormatPool.PURE_DATETIME_PATTERN));
        if (null != videoPathRelation && DateUtil.compare(nowStrmTime,
                videoPathRelation.getStrmTime(), DateFormatPool.PURE_DATETIME_PATTERN) == 0) {
            return;
        }
    }
}