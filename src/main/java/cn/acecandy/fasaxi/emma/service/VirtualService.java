package cn.acecandy.fasaxi.emma.service;

import cn.acecandy.fasaxi.emma.common.constants.CacheConstant;
import cn.acecandy.fasaxi.emma.config.EmbyConfig;
import cn.acecandy.fasaxi.emma.config.EmbyContentCacheReqWrapper;
import cn.acecandy.fasaxi.emma.dao.toolkit.entity.CustomCollections;
import cn.acecandy.fasaxi.emma.dao.toolkit.entity.MediaMetadata;
import cn.acecandy.fasaxi.emma.dao.toolkit.service.CustomCollectionsDao;
import cn.acecandy.fasaxi.emma.dao.toolkit.service.MediaMetadataDao;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.out.EmbyItem;
import cn.acecandy.fasaxi.emma.sao.out.EmbyItemsInfoOut;
import cn.acecandy.fasaxi.emma.sao.out.EmbyView;
import cn.acecandy.fasaxi.emma.sao.out.EmbyViewOut;
import cn.acecandy.fasaxi.emma.sao.proxy.EmbyProxy;
import cn.acecandy.fasaxi.emma.utils.CacheUtil;
import cn.acecandy.fasaxi.emma.utils.CloudUtil;
import cn.acecandy.fasaxi.emma.utils.ReUtil;
import cn.acecandy.fasaxi.emma.utils.ThreadUtil;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.core.collection.set.ConcurrentHashSet;
import cn.hutool.v7.core.date.DateUtil;
import cn.hutool.v7.core.map.MapUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.core.text.split.SplitUtil;
import cn.hutool.v7.core.util.ObjUtil;
import cn.hutool.v7.crypto.SecureUtil;
import cn.hutool.v7.http.client.Request;
import cn.hutool.v7.http.client.Response;
import cn.hutool.v7.http.client.engine.ClientEngine;
import cn.hutool.v7.http.meta.Method;
import cn.hutool.v7.http.server.servlet.ServletUtil;
import cn.hutool.v7.json.JSONArray;
import cn.hutool.v7.json.JSONObject;
import cn.hutool.v7.json.JSONUtil;
import com.mybatisflex.core.query.QueryColumn;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_200;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_401;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_500;
import static cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType.集合文件夹;
import static cn.acecandy.fasaxi.emma.dao.toolkit.entity.table.MediaMetadataTableDef.MEDIA_METADATA;

/**
 * 虚拟库服务
 *
 * @author AceCandy
 * @since 2024/11/19
 */
@Slf4j
@Component
public class VirtualService {

    /**
     * 虚拟id基准
     */
    private static final int MIMICKED_ID_BASE = 900000;

    /**
     * 库详细信息请求标记
     */
    private final Set<String> libDetailFlag = new ConcurrentHashSet<>();
    /**
     * 支持原生emby本地排序字段
     */
    private static final List<String> NATIVE_SORT_FIELDS = ListUtil.of("PremiereDate",
            "DateCreated", "CommunityRating", "ProductionYear", "SortName", "original", "DateLastContentAdded");

    /**
     * 元数据端点
     */
    private static final List<String> METADATA_ENDPOINTS = ListUtil.of(
            "/Items/Prefixes", // A-Z 首字母索引
            "/Genres",      // 类型筛选
            "/Studios",     // 工作室筛选
            "/Tags",         // 标签筛选
            "/OfficialRatings",// 官方评级筛选
            "/Years"          // 年份筛选
    );

    @Resource
    private Executor cacheRefreshExecutor;

    @Resource
    private OriginReqService originReqService;
    /**
     * 元数据 排序字段映射
     */
    private static final Map<String, QueryColumn> SORT_ORDER_MAP = Map.of(
            "PremiereDate", MEDIA_METADATA.RELEASE_DATE,
            "DateCreated", MEDIA_METADATA.DATE_ADDED,
            "CommunityRating", MEDIA_METADATA.RATING,
            "ProductionYear", MEDIA_METADATA.RELEASE_YEAR,
            "SortName", MEDIA_METADATA.TITLE);
    @Resource
    private CustomCollectionsDao customCollectionsDao;
    @Resource
    private MediaMetadataDao mediaMetadataDao;
    @Resource
    private EmbyProxy embyProxy;
    @Resource
    private EmbyConfig embyConfig;
    @Resource
    private RedisClient redisClient;

    @Resource
    private ClientEngine httpClient;

    @Resource
    private CloudUtil cloudUtil;

    @SneakyThrows
    public void handleViews(EmbyContentCacheReqWrapper request, HttpServletResponse response) {
        String userId = ReUtil.isViewUrl(request.getRequestURI());
        if (StrUtil.isBlank(userId)) {
            response.setStatus(CODE_401);
            return;
        }
        String cacheKey = CacheUtil.buildOriginViewsCacheKey(userId);
        List<EmbyView> fakeViewsItems = redisClient.getBean(cacheKey);
        if (fakeViewsItems != null) {
            // 异步刷新检查（防止重复刷新）
            if (!libDetailFlag.contains(cacheKey)) {
                Long ttl = redisClient.ttl(cacheKey);
                if (ttl != null && ttl > 0 && CacheConstant.DAY_30_S - ttl > 60) {
                    // 标记正在刷新，防止重复刷新
                    if (CollUtil.addIfAbsent(libDetailFlag, cacheKey)) {
                        CompletableFuture.runAsync(() -> {
                            try {
                                List<EmbyView> freshViews = buildViews(userId);
                                if (CollUtil.isNotEmpty(freshViews)) {
                                    redisClient.set(cacheKey, freshViews, CacheConstant.DAY_30_S);
                                }
                            } catch (Exception e) {
                                log.warn("异步刷新缓存失败: {}", cacheKey, e);
                            } finally {
                                libDetailFlag.remove(cacheKey);
                            }
                        }, cacheRefreshExecutor);
                    }
                }
            }
            EmbyViewOut finalLibs = EmbyViewOut.builder()
                    .items(fakeViewsItems)
                    .totalRecordCount(fakeViewsItems.size())
                    .build();
            response.setStatus(200);
            ServletUtil.write(response, JSONUtil.toJsonStr(finalLibs),
                    "application/json;charset=UTF-8");
            return;
        }

        fakeViewsItems = buildViews(userId);
        if (CollUtil.isEmpty(fakeViewsItems)) {
            response.setStatus(CODE_401);
            return;
        }
        redisClient.set(cacheKey, fakeViewsItems, CacheConstant.DAY_30_S);

        EmbyViewOut finalLibs = EmbyViewOut.builder()
                .items(fakeViewsItems)
                .totalRecordCount(fakeViewsItems.size())
                .build();
        response.setStatus(CODE_200);
        ServletUtil.write(response, JSONUtil.toJsonStr(finalLibs),
                "application/json;charset=UTF-8");
    }


    @SneakyThrows
    public void handleShowNext(EmbyContentCacheReqWrapper request, HttpServletResponse response) {
        String userId = request.getUserId();
        if (StrUtil.isBlank(userId)) {
            String token = request.getApiKey();
            if (StrUtil.isBlank(token)) {
                response.setStatus(CODE_401);
                return;
            }
            userId = "token_" + token;
        }
        String cacheKey = CacheUtil.buildOriginShowNextCacheKey(userId);
        EmbyItemsInfoOut embyItems = redisClient.getBean(cacheKey);

        if (embyItems != null) {
            // 异步刷新检查（防止重复刷新）
            if (!libDetailFlag.contains(cacheKey)) {
                Long ttl = redisClient.ttl(cacheKey);
                if (ttl != null && ttl > 0 && CacheConstant.DAY_30_S - ttl > 60) {
                    // 标记正在刷新，防止重复刷新
                    if (CollUtil.addIfAbsent(libDetailFlag, cacheKey)) {
                        refreshShowCacheAsync(request, userId);
                    }
                }
            }
            response.setStatus(200);
            ServletUtil.write(response, JSONUtil.toJsonStr(embyItems),
                    "application/json;charset=UTF-8");
            return;
        }

        // 缓存不存在，同步构建
        embyItems = JSONUtil.toBean(buildShowNext(request), EmbyItemsInfoOut.class);
        redisClient.set(cacheKey, embyItems, CacheConstant.DAY_30_S);
        if (null == embyItems) {
            response.setStatus(CODE_500);
            return;
        }
        response.setStatus(200);
        ServletUtil.write(response, JSONUtil.toJsonStr(embyItems),
                "application/json;charset=UTF-8");
    }

    @SneakyThrows
    private String buildShowNext(EmbyContentCacheReqWrapper request) {
        String url = embyConfig.getHost() + request.getParamUri();

        try (Response res = httpClient.send(Request.of(url).method(Method.GET)
                .header(request.getCachedHeader()))) {
            if (CODE_200.equals(res.getStatus())) {
                return res.bodyStr();
            }
        }
        return null;
    }

    /**
     * 刷新缓存异步
     *
     * @param request 请求
     * @param userId  用户ID
     */
    private void refreshShowCacheAsync(EmbyContentCacheReqWrapper request, String userId) {
        CompletableFuture.runAsync(() -> {
            String cacheKey = CacheUtil.buildOriginShowNextCacheKey(userId);
            try {
                EmbyItemsInfoOut freshData = JSONUtil.toBean(buildShowNext(request), EmbyItemsInfoOut.class);
                redisClient.set(cacheKey, freshData, CacheConstant.DAY_30_S);

                ThreadUtil.execVirtual(() -> cloudUtil.mkdirDeviceTmpDir(request.getDeviceId()));
            } catch (Exception e) {
                log.warn("异步刷新缓存失败: {}", cacheKey, e);
            } finally {
                // 移除刷新标记
                libDetailFlag.remove(cacheKey);
            }
        }, cacheRefreshExecutor);
    }

    @SneakyThrows
    public void handleUserResume(EmbyContentCacheReqWrapper request, HttpServletResponse response) {
        String userId = ReUtil.isResumeUrl(request.getRequestURI());
        if (StrUtil.isBlank(userId)) {
            response.setStatus(CODE_401);
            return;
        }
        if (StrUtil.startWith(request.getParentId(), "-")) {
            EmbyItemsInfoOut embyItems = EmbyItemsInfoOut.builder().items(ListUtil.of()).totalRecordCount(0).build();
            response.setStatus(CODE_200);
            ServletUtil.write(response, JSONUtil.toJsonStr(embyItems),
                    "application/json;charset=UTF-8");
            return;
        }
        handleShowNext(request, response);
    }

    /**
     * 构建用户视图
     *
     * @param userId 用户ID
     * @return {@link List }<{@link EmbyView }>
     */
    private List<EmbyView> buildViews(String userId) {
        EmbyViewOut originViews = embyProxy.getViews(userId);
        List<EmbyView> originLibs = originViews.getItems();
        if (CollUtil.isEmpty(originLibs)) {
            return originLibs;
        }
        List<CustomCollections> customCollections = customCollectionsDao.findAllByStatus(null);
        if (CollUtil.isEmpty(customCollections)) {
            return originLibs;
        }
        List<EmbyView> fakeViewsItems = ListUtil.of();

        for (CustomCollections coll : customCollections) {
            // 1. 物理检查：库在Emby里有实体吗？
            String realEmbyCollectionId = coll.getEmbyCollectionId();
            if (StrUtil.isBlank(realEmbyCollectionId)) {
                log.warn("[虚拟库<{}>] 被隐藏: 无对应Emby实体", coll.getName());
                continue;
            }
            // 2. 权限检查：用户在不在邀请函上？
            List<String> allowedUsers = JSONUtil.toList(coll.getAllowedUserIds(), String.class);
            if (CollUtil.isNotEmpty(allowedUsers) && !CollUtil.contains(allowedUsers, userId)) {
                log.warn("[虚拟库<{}>] 被隐藏: 用户不在可见列表中", coll.getName());
                continue;
            }

            // 生成虚拟库信息id
            String mimickedId = toMimickedId(coll.getId());

            JSONObject definition = JSONUtil.parseObj(coll.getDefinitionJson());
            JSONArray mergedLibs = definition.getJSONArray("merged_libraries");
            String nameSuffix = JSONUtil.isEmpty(mergedLibs) ? "" :
                    StrUtil.format(" (合并库: {}个)", mergedLibs.size());

            List<String> itemTypeFromDb = JSONUtil.toList(definition.getStr("item_type"), String.class);
            String collectionType = "movies";
            if (CollUtil.contains(itemTypeFromDb, "Series")) {
                collectionType = "tvshows";
            }
            String guid = SecureUtil.md5(mimickedId + embyConfig.getServerId() + coll.getName() + nameSuffix);
            String etag = SecureUtil.md5(embyConfig.getServerId() + coll.getName() + nameSuffix);
            fakeViewsItems.add(EmbyView.builder()
                    .id(mimickedId).serverId(embyConfig.getServerId()).name(coll.getName() + nameSuffix)
                    .guid(guid).etag(etag)
                    .dateCreated("2025-10-01T00:00:00.0000000Z").dateModified("2025-10-01T00:00:00.0000000Z")
                    .canDelete(false).canDownload(false).presentationUniqueKey(guid)
                    .sortName(coll.getName()).forcedSortName(coll.getName())
                    .externalUrls(ListUtil.of()).taglines(ListUtil.of()).remoteTrailers(ListUtil.of())
                    .providerIds(MapUtil.newHashMap()).isFolder(true).parentId("2")
                    .type(集合文件夹.getEmbyName())
                    .userData(Map.of("PlaybackPositionTicks", 0,
                            "IsFavorite", false,
                            "Played", false))
                    .childCount(1).displayPreferencesId(guid)
                    .primaryImageAspectRatio(1.7777777777777777).collectionType(collectionType)
                    .imageTags(Map.of("Primary", StrUtil.format(
                            "{}?timestamp={}", realEmbyCollectionId, DateUtil.currentSeconds())))
                    .backdropImageTags(ListUtil.of()).lockedFields(ListUtil.of()).lockData(false)
                    .build());
        }
        log.info("已为用户 {} 生成 {} 个可见的虚拟库", userId, fakeViewsItems.size());

        // 原生库合并逻辑
        List<EmbyView> nativeRetainLibs = originLibs.stream().filter(v ->
                embyConfig.getVirtual().containsKey(v.getId())).toList();
        fakeViewsItems.addAll(nativeRetainLibs);

        return fakeViewsItems;
    }

    @SneakyThrows
    public void handleLatest(EmbyContentCacheReqWrapper request, HttpServletResponse response) {
        String userId = ReUtil.isLatestUrl(request.getRequestURI());
        if (StrUtil.isBlank(userId)) {
            response.setStatus(CODE_401);
            return;
        }
        EmbyItemsInfoOut embyItems = buildLibDetailByCache(request);
        response.setStatus(CODE_200);
        ServletUtil.write(response, JSONUtil.toJsonStr(embyItems.getItems()),
                "application/json;charset=UTF-8");
    }

    /**
     * 通过缓存构建库详情
     *
     * @return {@link EmbyItemsInfoOut }
     */
    private EmbyItemsInfoOut buildLibDetailByCache(EmbyContentCacheReqWrapper request) {
        String cacheKey = CacheUtil.buildOriginLatestCacheKey(request);
        EmbyItemsInfoOut embyItems = redisClient.getBean(cacheKey);

        if (embyItems != null) {
            // 异步刷新检查（防止重复刷新）
            if (!libDetailFlag.contains(cacheKey)) {
                Long ttl = redisClient.ttl(cacheKey);
                if (ttl != null && ttl > 0 && CacheConstant.DAY_30_S - ttl > 60) {
                    // 标记正在刷新，防止重复刷新
                    if (CollUtil.addIfAbsent(libDetailFlag, cacheKey)) {
                        refreshLatestCacheAsync(request);
                    }
                }
            }
            return embyItems;
        }

        // 缓存不存在，同步构建
        embyItems = buildLibDetail(request);
        redisClient.set(cacheKey, embyItems, CacheConstant.DAY_30_S);
        return embyItems;
    }

    /**
     * 刷新缓存异步
     *
     */
    private void refreshLatestCacheAsync(EmbyContentCacheReqWrapper request) {
        CompletableFuture.runAsync(() -> {
            String cacheKey = CacheUtil.buildOriginLatestCacheKey(request);
            try {
                EmbyItemsInfoOut freshData = buildLibDetail(request);
                redisClient.set(cacheKey, freshData, CacheConstant.DAY_30_S);
            } catch (Exception e) {
                log.warn("异步刷新缓存失败: {}", cacheKey, e);
            } finally {
                // 移除刷新标记
                libDetailFlag.remove(cacheKey);
            }
        }, cacheRefreshExecutor);
    }

    private EmbyItemsInfoOut buildLibDetail(EmbyContentCacheReqWrapper request) {
        Map<String, Object> cachedParam = request.getCachedParam();
        String userId = request.getUserId();
        String parentId = request.getParentId();

        if (!StrUtil.startWith(parentId, "-")) {
            List<EmbyItem> items = JSONUtil.toList(buildShowNext(request), EmbyItem.class);
            return EmbyItemsInfoOut.builder().items(items).totalRecordCount(items.size()).build();
        }
        Long realId = fromMimickedId(parentId);
        CustomCollections coll = customCollectionsDao.getById(realId);
        if (coll == null) {
            return EmbyItemsInfoOut.builder().items(ListUtil.of()).totalRecordCount(0).build();
        }
        JSONArray generatedMediaInfoJson = JSONUtil.parseArray(coll.getGeneratedMediaInfoJson());
        List<String> embyIds = generatedMediaInfoJson.stream().filter(g ->
                        ObjUtil.isNotEmpty(g.getObjByPath("emby_id")))
                .map(g -> g.getByPath("emby_id", String.class).toString()).toList();
        if (CollUtil.isEmpty(embyIds)) {
            return EmbyItemsInfoOut.builder().items(ListUtil.of()).totalRecordCount(0).build();
        }
        JSONObject definition = JSONUtil.parseObj(coll.getDefinitionJson());
        if (definition.getBool("enforce_emby_permissions", false)) {
            // 强制emby原生权限验证(默认只展示未观看完的)
            List<String> allItemIds = getAllItemsIdByCache();
            embyIds = embyIds.stream().filter(allItemIds::contains).toList();
            if (CollUtil.isEmpty(embyIds)) {
                return EmbyItemsInfoOut.builder().items(ListUtil.of()).totalRecordCount(0).build();
            }
        }
        // 判断库类型
        List<String> itemTypeFromDb = JSONUtil.toList(definition.getStr("item_type"), String.class);
        boolean isSeriesFocused = itemTypeFromDb.contains("Series");
        // 对于任何包含剧集的库（纯剧集或混合库），使用“综合排名”；对于纯电影库，简单按入库时间即可
        List<String> sortStr = isSeriesFocused ?
                ListUtil.of("DateLastContentAdded", "SortName") : ListUtil.of("DateCreated", "SortName");
        // 最新是降序
        String sortOrder = "Descending";

        // 2. 判断走本地排序还是Emby原生排序
        boolean useNativeSort = false;
        if (CollUtil.containsAll(NATIVE_SORT_FIELDS, sortStr) ||
                StrUtil.equals(definition.getStr("default_sort_by"), "none")) {
            useNativeSort = true;
        }
        int start = MapUtil.getInt(cachedParam, "StartIndex", 0);
        int showLimit = MapUtil.getInt(cachedParam, "Limit", 24);
        String fields = MapUtil.getStr(cachedParam, "Fields",
                "PrimaryImageAspectRatio,BasicSyncInfo,UserData,ProductionYear");

        if (useNativeSort) {
            // Emby原生排序
            return embyProxy.getUserItems(userId, embyIds, sortStr, sortOrder,
                    start, showLimit, fields, MapUtil.getStr(cachedParam, "IncludeItemTypes"));
        } else {
            log.warn("非原生排序---->这里按理说不能进入");
            List<QueryColumn> dbSortStr = sortStr.stream().map(SORT_ORDER_MAP::get).toList();
            boolean dbSortOrder = !StrUtil.equals(sortOrder, "Descending");
            List<MediaMetadata> metadataList = mediaMetadataDao.findByEmbyIdOrder(
                    embyIds, dbSortStr, dbSortOrder, showLimit);
            List<String> sortEmbyIds = metadataList.stream().map(MediaMetadata::getEmbyItemIdsJson)
                    .filter(CollUtil::isNotEmpty).flatMap(List::stream).filter(StrUtil::isNotBlank)
                    .toList();
            return embyProxy.getUserItems(userId, sortEmbyIds, null, null,
                    null, null, fields, null);
        }
    }

    /**
     * 通过缓存获取所有项目id
     *
     * @return {@link List }<{@link String }>
     */
    private List<String> getAllItemsIdByCache() {
        List<String> allItemsId = ListUtil.ofCopyOnWrite();
        embyConfig.getVirtualHide().keySet().parallelStream().forEach(k -> {
            String result = redisClient.getStr(CacheUtil.buildItemsIdCacheKey(k));
            allItemsId.addAll(SplitUtil.splitTrim(result, ","));
        });
        return allItemsId;
    }

    @SneakyThrows
    public void handleItems(EmbyContentCacheReqWrapper request, HttpServletResponse response) {
        String userId = ReUtil.getUserByItemsUrl(request.getRequestURI());
        if (StrUtil.isBlank(userId)) {
            response.setStatus(CODE_401);
            return;
        }
        EmbyItemsInfoOut embyItems = buildLibDetailByCache(request);
        response.setStatus(CODE_200);
        ServletUtil.write(response, JSONUtil.toJsonStr(embyItems),
                "application/json;charset=UTF-8");
    }

    @SneakyThrows
    public void handleImage(EmbyContentCacheReqWrapper request, HttpServletResponse response) {
        String tag = MapUtil.getStr(request.getCachedParam(), "tag");
        if (StrUtil.isBlank(tag)) {
            response.setStatus(CODE_401);
            return;
        }
        String realEmbyCollectionId = CollUtil.getFirst(SplitUtil.splitTrim(tag, "?"));
        originReqService.return308to200(response, StrUtil.format("/Items/{}/Images/Primary?maxWidth={}&quality=90",
                realEmbyCollectionId, MapUtil.getInt(request.getCachedParam(), "maxWidth")));
    }

    @SneakyThrows
    public void handleOtherEndpoint(EmbyContentCacheReqWrapper request, HttpServletResponse response) {
        String uri = request.getRequestURI();
        if (!StrUtil.endWithAny(uri, METADATA_ENDPOINTS.toArray(new String[0]))) {
            response.setStatus(CODE_401);
            return;
        }
        String parentId = request.getParentId();
        Long realId = fromMimickedId(parentId);
        CustomCollections customCollections = customCollectionsDao.getById(realId);
        if (null == customCollections) {
            response.setStatus(CODE_401);
            return;
        }
        String realEmbyCollectionId = customCollections.getEmbyCollectionId();
        String url = embyConfig.getHost() + StrUtil.replace(request.getParamUri(), parentId, realEmbyCollectionId);
        Request originalRequest = Request.of(url).method(Method.valueOf(request.getMethod()));
        try (Response res = httpClient.send(originalRequest)) {
            response.setStatus(res.getStatus());
            ServletUtil.write(response, res.bodyStream(),
                    "application/json;charset=UTF-8");
        }
    }

    /**
     * 构建虚拟id
     *
     * @param dbId 数据库id
     * @return {@link String }
     */
    private String toMimickedId(Long dbId) {
        return String.valueOf(-(MIMICKED_ID_BASE + dbId));
    }

    /**
     * 通过虚拟id解析embyId
     *
     * @param mimickedId 模仿id
     * @return {@link Long }
     */
    private Long fromMimickedId(String mimickedId) {
        return (long) (-(Integer.parseInt(mimickedId)) - MIMICKED_ID_BASE);
    }
}