package cn.acecandy.fasaxi.emma.service;

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
import cn.acecandy.fasaxi.emma.utils.ReUtil;
import cn.acecandy.fasaxi.emma.utils.ThreadUtil;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.core.date.DateUtil;
import cn.hutool.v7.core.map.MapUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.core.text.split.SplitUtil;
import cn.hutool.v7.core.util.ObjUtil;
import cn.hutool.v7.crypto.SecureUtil;
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

import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_200;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_401;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.DAY_7_S;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.MINUTE_30_S;
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
     * 支持原生emby本地排序字段
     */
    private static final List<String> NATIVE_SORT_FIELDS = ListUtil.of("PremiereDate",
            "DateCreated", "CommunityRating", "ProductionYear", "SortName", "original", "DateLastContentAdded");
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

    public static void main(String[] args) {
        String s = "{\n" +
                "      \"Guid\": \"8381d29afe8546b0b0199366d4f8855d\",\n" +
                "      \"Etag\": \"29d428cad00374d9fedcdc3956b03d83\",\n" +
                "      \"DateCreated\": \"2024-09-01T15:24:40.0000000Z\",\n" +
                "      \"DateModified\": \"0001-01-01T00:00:00.0000000Z\",\n" +
                "      \"CanDelete\": false,\n" +
                "      \"CanDownload\": false,\n" +
                "      \"SortName\": \"\uD83C\uDFAC 华语电影\",\n" +
                "      \"ForcedSortName\": \"\uD83C\uDFAC 华语电影\",\n" +
                "      \"ExternalUrls\": [],\n" +
                "      \"Taglines\": [],\n" +
                "      \"RemoteTrailers\": [],\n" +
                "      \"ChildCount\": 1,\n" +
                "      \"DisplayPreferencesId\": \"8381d29afe8546b0b0199366d4f8855d\",\n" +
                "      \"CollectionType\": \"movies\",\n" +
                "      \"LockedFields\": [],\n" +
                "      \"LockData\": false\n" +
                "    }";
        EmbyItem embyItem = JSONUtil.toBean(s, EmbyItem.class);
        System.out.println(JSONUtil.toJsonStr(embyItem));
    }

    @SneakyThrows
    public void handleViews(EmbyContentCacheReqWrapper request, HttpServletResponse response) {
        String userId = ReUtil.isViewUrl(request.getRequestURI());
        if (StrUtil.isBlank(userId)) {
            response.setStatus(CODE_401);
            return;
        }
        List<EmbyView> fakeViewsItems = buildViews(userId);
        if (CollUtil.isEmpty(fakeViewsItems)) {
            response.setStatus(CODE_401);
            return;
        }
        EmbyViewOut finalLibs = EmbyViewOut.builder()
                .items(fakeViewsItems)
                .totalRecordCount(fakeViewsItems.size())
                .build();
        response.setStatus(CODE_200);
        ServletUtil.write(response, JSONUtil.toJsonStr(finalLibs),
                "application/json;charset=UTF-8");
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
    public void handleResume(EmbyContentCacheReqWrapper request, HttpServletResponse response) {
        String userId = ReUtil.isResumeUrl(request.getRequestURI());
        if (StrUtil.isBlank(userId)) {
            response.setStatus(CODE_401);
            return;
        }
        EmbyItemsInfoOut embyItems = EmbyItemsInfoOut.builder().items(ListUtil.of()).totalRecordCount(0).build();
        response.setStatus(CODE_200);
        ServletUtil.write(response, JSONUtil.toJsonStr(embyItems),
                "application/json;charset=UTF-8");
    }

    @SneakyThrows
    public void handleLatest(EmbyContentCacheReqWrapper request, HttpServletResponse response) {
        String userId = ReUtil.isLatestUrl(request.getRequestURI());
        if (StrUtil.isBlank(userId)) {
            response.setStatus(CODE_401);
            return;
        }
        String parentId = request.getParentId();
        EmbyItemsInfoOut embyItems = buildLibDetail(request.getCachedParam(), userId, parentId);
        response.setStatus(CODE_200);
        ServletUtil.write(response, JSONUtil.toJsonStr(embyItems.getItems()),
                "application/json;charset=UTF-8");
    }

    private EmbyItemsInfoOut buildLibDetail(Map<String, Object> cachedParam,
                                            String userId, String parentId) {
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
            List<String> sortEmbyIds = metadataList.stream().map(MediaMetadata::getEmbyItemId).toList();
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

    /**
     * 通过缓存获取用户拥有权限的id
     * <p>
     * 弃用 使用getAllItemsIdByCache获取所有的id之后 最后再通过用户接口进行输出可直接过滤掉没有权限的id
     *
     * @param userId 用户ID
     * @return {@link List }<{@link String }>
     */
    @Deprecated
    private List<String> getUserAccessIdsByCache(String userId) {
        String cacheKey = CacheUtil.buildUserPermsCacheKey(userId);
        String result = redisClient.getStr(cacheKey);
        if (StrUtil.isNotBlank(result)) {
            List<String> resultList = SplitUtil.splitTrim(result, "||");
            try {
                return SplitUtil.splitTrim(CollUtil.getLast(resultList), ",");
            } finally {
                if (DateUtil.currentSeconds() - MINUTE_30_S > Long.parseLong(CollUtil.getFirst(resultList))) {
                    ThreadUtil.execute(() -> {
                        List<String> itemIds = embyProxy.getUserPermsEmbyIdOnLock(userId);
                        if (CollUtil.isNotEmpty(itemIds)) {
                            redisClient.set(cacheKey, StrUtil.format("{}||{}",
                                    DateUtil.currentSeconds(), StrUtil.join(",", itemIds)), DAY_7_S);
                        }
                    });
                }
            }
        }
        List<String> itemIds = embyProxy.getUserPermsEmbyIdOnLock(userId);
        if (CollUtil.isNotEmpty(itemIds)) {
            redisClient.set(cacheKey, StrUtil.format("{}||{}",
                    DateUtil.currentSeconds(), StrUtil.join(",", itemIds)), DAY_7_S);
        }
        return itemIds;
    }

    @SneakyThrows
    public void handleItems(EmbyContentCacheReqWrapper request, HttpServletResponse response) {
        String userId = ReUtil.getUserByItemsUrl(request.getRequestURI());
        if (StrUtil.isBlank(userId)) {
            response.setStatus(CODE_401);
            return;
        }
        String parentId = request.getParentId();
        EmbyItemsInfoOut embyItems = buildLibDetail(request.getCachedParam(), userId, parentId);
        response.setStatus(CODE_200);
        ServletUtil.write(response, JSONUtil.toJsonStr(embyItems),
                "application/json;charset=UTF-8");
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