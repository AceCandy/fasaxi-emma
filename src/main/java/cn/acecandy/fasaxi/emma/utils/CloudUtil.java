package cn.acecandy.fasaxi.emma.utils;

import cn.acecandy.fasaxi.emma.common.enums.CloudStorageType;
import cn.acecandy.fasaxi.emma.config.EmbyConfig;
import cn.acecandy.fasaxi.emma.config.OpConfig;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.client.RedisLockClient;
import cn.acecandy.fasaxi.emma.sao.dto.Rile;
import cn.acecandy.fasaxi.emma.sao.out.R115Search;
import cn.acecandy.fasaxi.emma.sao.out.R115SearchFileReq;
import cn.acecandy.fasaxi.emma.sao.out.R115SearchFileResp;
import cn.acecandy.fasaxi.emma.sao.proxy.OpProxy;
import cn.acecandy.fasaxi.emma.sao.proxy.R115Proxy;
import cn.acecandy.fasaxi.emma.sao.proxy.R123Proxy;
import cn.acecandy.fasaxi.emma.sao.proxy.R123ZongProxy;
import cn.acecandy.fasaxi.emma.service.VideoRedirectService;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.core.date.DateUtil;
import cn.hutool.v7.core.io.file.FileNameUtil;
import cn.hutool.v7.core.io.file.FileUtil;
import cn.hutool.v7.core.lang.Console;
import cn.hutool.v7.core.net.url.UrlDecoder;
import cn.hutool.v7.core.net.url.UrlPath;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.core.util.CharsetUtil;
import cn.hutool.v7.http.client.Request;
import cn.hutool.v7.http.client.Response;
import cn.hutool.v7.http.meta.Method;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.acecandy.fasaxi.emma.common.enums.CloudStorageType.R_115;
import static cn.acecandy.fasaxi.emma.common.enums.CloudStorageType.R_123;
import static cn.acecandy.fasaxi.emma.sao.client.RedisLockClient.buildDeviceLock;
import static cn.acecandy.fasaxi.emma.utils.CacheUtil.buildDeviceFileId115Key;

/**
 * 云盘工具类
 *
 * @author tangningzhu
 * @since 2025/8/19
 */
@Slf4j
@Component
public final class CloudUtil {
    final String TEMP_DIR = "0-临时";
    @Resource
    private R115Proxy r115Proxy;
    @Resource
    private R123Proxy r123Proxy;
    @Resource
    private R123ZongProxy r123ZongProxy;
    @Resource
    private RedisClient redisClient;
    @Resource
    private RedisLockClient redisLockClient;
    @Resource
    private OpProxy opProxy;
    @Resource
    private OpConfig opConfig;
    @Resource
    private EmbyConfig embyConfig;

    static void main(String[] args) {
        String filePath = "//Users/mac/Library//CloudStorage\\OneDrive-%E4%B8%AA%E4%BA%BA%2Fworkspace%2Fgithub%2Fjava%2Ffasaxi-emma%2Fsrc%2Fmain%2Fjava%2Fcn%2Facecandy%2Ffasaxi%2Femma%2Futils%2FCloudUtil.java";
        Console.log(filePath);
        filePath = FileUtil.normalize(filePath);
        Console.log(filePath);
        Console.log(UrlPath.of(filePath, CharsetUtil.defaultCharset()).getSegments());
    }

    /**
     * 获取文件
     *
     * @param cloudStorage 云存储
     * @param filePath     文件路径
     * @return {@link Rile }
     */
    public Rile getFile(CloudStorageType cloudStorage, String filePath) {
        if (null == cloudStorage || StrUtil.isBlank(filePath)) {
            return null;
        }
        Long parentId = 0L;
        filePath = FileUtil.normalize(UrlDecoder.decode(filePath));

        // 处理缓存逻辑，减少类型转换
        Map.Entry<String, Rile> entry = CloudPathCacheUtil.find(cloudStorage, filePath);
        if (entry != null) {
            parentId = entry.getValue().getFileId();
            String keyPath = StrUtil.removePrefix(entry.getKey(), cloudStorage + ":");
            filePath = StrUtil.removePrefix(filePath, keyPath);
        }

        // 拆分路径并过滤空片段
        List<CharSequence> segments = UrlPath.of(filePath, Charset.defaultCharset()).getSegments();

        Rile file = null;
        for (CharSequence segment : segments) {
            List<Rile> findFileList = ListUtil.of();
            if (cloudStorage.equals(R_123)) {
                findFileList = r123Proxy.listRiles(parentId, segment);
            } else if (cloudStorage.equals(CloudStorageType.R_123_ZONG)) {
                findFileList = r123ZongProxy.listRiles(parentId, segment);
            } else if (cloudStorage.equals(R_115)) {
                // findFileList = r115Proxy.listRiles(parentId, segment);
                findFileList = r115Proxy.getRiles(filePath);
                file = CollUtil.getFirst(findFileList);
                return file;
            }
            if (CollUtil.isEmpty(findFileList)) {
                file = null;
                log.warn("[{}云盘]未能查找到[{}]，完整文件路径:{}", cloudStorage, segment, filePath);
                break;
            }
            file = CollUtil.getFirst(findFileList);
            // 直接使用已获取的file对象，减少重复获取
            parentId = file.getFileId();
        }
        return file;
    }

    /**
     * 获取文件(匹配模式)
     *
     * @param cloudStorage 云存储
     * @param filePath     文件路径
     * @return {@link Rile }
     */
    public Rile getFileMatch(CloudStorageType cloudStorage, String filePath, long size) {
        filePath = FileUtil.normalize(UrlDecoder.decode(filePath));
        List<CharSequence> segments = UrlPath.of(filePath, Charset.defaultCharset()).getSegments();
        CharSequence segment = CollUtil.getLast(segments);

        List<Rile> findFileList = ListUtil.of();
        if (cloudStorage.equals(R_123)) {
            findFileList = r123Proxy.listRiles(segment);
            findFileList = findFileList.stream().filter(item -> item.getFileSize() == size).toList();
        } else if (cloudStorage.equals(CloudStorageType.R_123_ZONG)) {
            findFileList = r123ZongProxy.listRiles(segment);
            findFileList = findFileList.stream().filter(item -> item.getFileSize() == size).toList();
        } else if (cloudStorage.equals(R_115)) {
            // findFileList = r115Proxy.listRiles(segment);
            findFileList = r115Proxy.getRiles(filePath);
        }
        if (CollUtil.isEmpty(findFileList)) {
            log.warn("[{}云盘]未能查找到[{}]，完整文件路径:{}", cloudStorage, segment, filePath);
            return null;
        }
        return CollUtil.getFirst(findFileList);
    }

    public Rile getFileByCache(CloudStorageType cloudStorage, String filePath, long size) {
        String cacheKey = CacheUtil.buildCloudSearchKey(cloudStorage, filePath, size);
        Rile rile = redisClient.getBean(cacheKey);
        if (null != rile) {
            return rile;
        }

        if (size < 1000) {
            rile = getFile(cloudStorage, filePath);
        } else {
            rile = getFileMatch(cloudStorage, filePath, size);
        }
        redisClient.setBean(cacheKey, rile, 60 * 60 * 6);
        return rile;
    }

    public Rile getFileByCacheOnDevice(CloudStorageType cloudStorage, String deviceId,
                                       String filePath, long size) {
        String cacheKey = CacheUtil.buildCloudSearchKey(cloudStorage, deviceId, filePath, size);
        Rile rile = redisClient.getBean(cacheKey);
        if (null != rile) {
            return rile;
        }
        if (size < 1000 || StrUtil.startWith(FileNameUtil.mainName(filePath), "S0")) {
            rile = getFile(cloudStorage, filePath);
        } else {
            rile = getFileMatch(cloudStorage, filePath, size);
        }
        redisClient.setBean(cacheKey, rile, 60 * 60 * 6);
        return rile;
    }

    public Rile getFileByCopyCacheOnDevice(CloudStorageType cloudStorage, String deviceId,
                                           String filePath, long size) {
        String cacheKey = CacheUtil.buildCloudSearchKey(cloudStorage, deviceId, filePath, size);
        Rile rile = redisClient.getBean(cacheKey);
        if (null != rile) {
            return rile;
        }
        if (R_115.equals(cloudStorage)) {
            String copyPath = FileUtil.normalize(UrlDecoder.decode(filePath));
            copyPath = StrUtil.format("/0-临时/{}/{}", deviceId, FileNameUtil.getName(copyPath));
            rile = getFile(cloudStorage, copyPath);
        }
        redisClient.setBean(cacheKey, rile, 60 * 60 * 6);
        return rile;
    }

    /**
     * 获取下载链接
     *
     * @param cloudStorage 云存储
     * @param filePath     文件路径
     * @return {@link Rile }
     */
    public String getDownloadUrl(CloudStorageType cloudStorage, String ua, String filePath, long size) {
        Rile rile = getFileByCache(cloudStorage, filePath, size);
        if (null == rile) {
            return null;
        }
        Long rileId = rile.getFileId();
        String downloadUrl = null;
        if (cloudStorage.equals(R_123)) {
            downloadUrl = r123Proxy.getDownloadUrl(ua, rileId);
        } else if (cloudStorage.equals(CloudStorageType.R_123_ZONG)) {
            downloadUrl = r123ZongProxy.getDownloadUrl(ua, rileId);
        } else if (cloudStorage.equals(R_115)) {
            downloadUrl = r115Proxy.getDownloadUrl(ua, rile.getPickCode());
        }
        return downloadUrl;
    }

    /**
     * 获取下载链接
     *
     * @param cloudStorage 云存储
     * @param filePath     文件路径
     * @return {@link Rile }
     */
    public String getDownloadUrlOnCopy(CloudStorageType cloudStorage,
                                       String ua, String deviceId, String filePath, long size) {
        Rile rile = getFileByCopyCacheOnDevice(cloudStorage, deviceId, filePath, size);
        if (null != rile) {
            Long rileId = rile.getFileId();
            String downloadUrl = null;
            if (cloudStorage.equals(R_123)) {
                downloadUrl = r123Proxy.getDownloadUrl(ua, rileId);
            } else if (cloudStorage.equals(CloudStorageType.R_123_ZONG)) {
                downloadUrl = r123ZongProxy.getDownloadUrl(ua, rileId);
            } else if (cloudStorage.equals(R_115)) {
                downloadUrl = r115Proxy.getDownloadUrl(ua, rile.getPickCode());
            }
            return downloadUrl;
        }
        rile = getFileByCacheOnDevice(cloudStorage, deviceId, filePath, size);
        if (null == rile) {
            return null;
        }
        Long rileId = rile.getFileId();
        String downloadUrl = null;
        if (cloudStorage.equals(R_123)) {
            downloadUrl = r123Proxy.getDownloadUrl(ua, rileId);
        } else if (cloudStorage.equals(CloudStorageType.R_123_ZONG)) {
            downloadUrl = r123ZongProxy.getDownloadUrl(ua, rileId);
        } else if (cloudStorage.equals(R_115)) {
            Long copyToDir = getDeviceTmpDir(deviceId);
            ThreadUtil.execVirtual(() -> r115Proxy.copyFile(rileId, copyToDir));
            ThreadUtil.safeSleep(80);

            R115Search<List<R115SearchFileResp>> searchFile = null;
            do {
                searchFile = r115Proxy.searchFile(
                        R115SearchFileReq.builder().search_value(rile.getFileName())
                                .cid(copyToDir).limit(1).fc(2).type(4)
                                .suffix(FileNameUtil.getSuffix(rile.getFileName())).build());
                ThreadUtil.safeSleep(120);
            } while (CollUtil.isEmpty(searchFile.getData()));
            downloadUrl = r115Proxy.getDownloadUrl(ua, CollUtil.getFirst(searchFile.getData()).getPick_code());
        }
        return downloadUrl;
    }

    /**
     * 获取下载链接(通过openlist)
     *
     * @param cloudStorage 云存储
     * @param filePath     文件路径
     * @return {@link Rile }
     */
    public String getDownloadUrlOnCopyByOpenlist(CloudStorageType cloudStorage,
                                                 String ua, String deviceId, String filePath) {
        // 1. 解码并规范化原始文件路径（处理URL编码和路径格式）
        String decodedNormalizedPath = FileUtil.normalize(UrlDecoder.decode(filePath));

        // 单独处理 R_115 类型的云存储
        /*if (cloudStorage.equals(R_115)) {
            String real302Url = processR115Storage(cloudStorage, ua, deviceId, decodedNormalizedPath);
            if (StrUtil.isNotBlank(real302Url)) {
                return real302Url;
            }
        }*/

        // 构建默认的媒体路径（适用于所有类型，R_115已通过processedFilePath处理）
        String defaultMediaPath = cloudStorage.getPrefix() + decodedNormalizedPath;
        return redirect302ByOpenlist(cloudStorage, defaultMediaPath, ua);
    }

    /**
     * 处理 R_115 类型云存储的路径构建和文件复制逻辑
     */
    private String processR115Storage(CloudStorageType cloudStorage, String ua, String deviceId,
                                      String decodedNormalizedPath) {
        // 2. 构建包含文件名的完整复制路径（格式：主路径/临时目录/日期/设备ID/文件路径）
        String copyFullPath = StrUtil.format("{}/{}/{}/{}{}",
                cloudStorage.getMain(), TEMP_DIR, DateUtil.formatToday(), deviceId, decodedNormalizedPath);

        // 3. 拆分复制路径为目录和文件名（用于创建目录和复制文件）
        String fileName = FileNameUtil.getName(copyFullPath);
        // 仅目录部分
        String copyDirectoryPath = StrUtil.removeSuffix(copyFullPath, fileName);

        // 4. 尝试获取重定向URL，如果存在则直接返回
        String r115MediaPath = opConfig.getDHost() + copyFullPath;
        String real302Url = redirect302ByOpenlist(cloudStorage, r115MediaPath, ua);
        if (StrUtil.isNotBlank(real302Url)) {
            return real302Url;
        }

        // 5. 若重定向URL不存在，异步创建目录并复制文件（确保后续可访问）
        ThreadUtil.execute(() -> {
            if (opProxy.mkdir(copyDirectoryPath)) { // 先创建目录
                // 源文件目录（从原始路径中移除文件名）
                String sourceDirectory = StrUtil.removeSuffix(
                        cloudStorage.getMain() + decodedNormalizedPath, fileName);
                opProxy.copy(sourceDirectory, copyDirectoryPath, Collections.singletonList(fileName));
            }
        });

        // 6. 返回处理后的文件路径（供默认路径使用）
        return real302Url;
    }

    /**
     * 通过openlist重定向
     *
     * @param cloudStorage 云存储
     * @param newMediaPath 新媒体之路
     * @return {@link String }
     */
    public String redirect302ByOpenlist(CloudStorageType cloudStorage,
                                        String newMediaPath, String ua) {
        newMediaPath = StrUtil.replace(newMediaPath, "http://192.168.1.249:5244", opConfig.getHost());
        String real302Url = "";
        try (Response resp = Request.of(newMediaPath).method(Method.HEAD)
                .header("User-Agent", ua).send()) {
            real302Url = resp.header("Location");
        } catch (Exception e) {
            log.warn("获取<{}>重定向URL失败: {}", cloudStorage.getValue(), newMediaPath, e);
        }
        return real302Url;
    }

    /**
     * 生成临时文件夹
     *
     * @param deviceId 设备标识符
     */
    public void mkdirDeviceTmpDir(String deviceId) {
        if (StrUtil.isBlank(deviceId)) {
            return;
        }
        String lockKey = buildDeviceLock(deviceId);
        if (!redisLockClient.lock(lockKey)) {
            return;
        }
        try {
            getDeviceTmpDirByOpenlist(deviceId);
        } finally {
            redisLockClient.unlock(lockKey);
        }
    }

    /**
     * 生成临时文件夹
     *
     * @param deviceId 设备标识符
     */
    private Long getDeviceTmpDir(String deviceId) {
        if (StrUtil.isBlank(deviceId)) {
            return null;
        }
        String cacheKey = buildDeviceFileId115Key(deviceId);
        Object fileId = redisClient.get(cacheKey);
        if (fileId != null) {
            return (Long) fileId;
        }

        Long copyToDir = r115Proxy.addFolder(deviceId);
        redisClient.set(cacheKey, copyToDir, 60 * 60 * 24 * 7);
        return copyToDir;
    }

    /**
     * 生成临时文件夹
     *
     * @param deviceId 设备标识符
     */
    private void getDeviceTmpDirByOpenlist(String deviceId) {
        if (StrUtil.isBlank(deviceId)) {
            return;
        }
        String cacheKey = buildDeviceFileId115Key(deviceId);
        Object fileId = redisClient.get(cacheKey);
        if (fileId != null) {
            return;
        }
        if (opProxy.mkdir(StrUtil.format("/new115/0-临时/{}/{}",
                DateUtil.formatToday(), deviceId))) {
            redisClient.set(cacheKey, 1, 60 * 60 * 24 * 2);
        }
    }

    /**
     * 缓存302 url
     *
     * @param result        结果
     * @param mediaSourceId 媒体源id
     * @param deviceId      设备标识符
     */
    public void cacheOpenList302Url(VideoRedirectService.RedirectResult result, String mediaSourceId, String deviceId) {
        // 构建缓存key
        String cacheKey = CacheUtil.buildVideoCacheKey(result.storageType(), mediaSourceId, deviceId);

        // 设置Redis缓存
        String cacheValue = result.storageType() + "|" + result.url();
        redisClient.set(cacheKey, cacheValue, result.expireTime());
    }

    /**
     * 请求和缓存302 url
     *
     * @param cloudStorage  云存储
     * @param newMediaPath  新媒体之路
     * @param ua            ua
     * @param mediaSourceId 媒体源id
     * @param deviceId      设备标识符
     */
    public String reqAndCacheOpenList302Url(CloudStorageType cloudStorage, String newMediaPath, String ua,
                                            String mediaSourceId, String deviceId) {
        if (cloudStorage != R_115 && cloudStorage != R_123) {
            return newMediaPath;
        }
        String cacheUrl = redisClient.getStrFindOne(CacheUtil.buildVideoCacheKeyList(mediaSourceId, deviceId));
        if (StrUtil.isNotBlank(cacheUrl)) {
            return cacheUrl;
        }
        String url302 = redirect302ByOpenlist(cloudStorage, newMediaPath, ua);
        cacheOpenList302Url(new VideoRedirectService.RedirectResult(url302, cloudStorage.getValue(), CacheUtil.getVideoDefaultExpireTime(), newMediaPath),
                mediaSourceId, deviceId);
        return url302;
    }
}