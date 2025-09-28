package cn.acecandy.fasaxi.emma.utils;

import cn.acecandy.fasaxi.emma.common.enums.CloudStorageType;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.dto.Rile;
import cn.acecandy.fasaxi.emma.sao.out.R115Search;
import cn.acecandy.fasaxi.emma.sao.out.R115SearchFileReq;
import cn.acecandy.fasaxi.emma.sao.out.R115SearchFileResp;
import cn.acecandy.fasaxi.emma.sao.proxy.R115Proxy;
import cn.acecandy.fasaxi.emma.sao.proxy.R123Proxy;
import cn.acecandy.fasaxi.emma.sao.proxy.R123ZongProxy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.collection.ListUtil;
import org.dromara.hutool.core.io.file.FileNameUtil;
import org.dromara.hutool.core.io.file.FileUtil;
import org.dromara.hutool.core.lang.Console;
import org.dromara.hutool.core.net.url.UrlDecoder;
import org.dromara.hutool.core.net.url.UrlPath;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.core.util.CharsetUtil;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import static cn.acecandy.fasaxi.emma.common.enums.CloudStorageType.R_115;
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
    @Resource
    private R115Proxy r115Proxy;

    @Resource
    private R123Proxy r123Proxy;

    @Resource
    private R123ZongProxy r123ZongProxy;

    @Resource
    private RedisClient redisClient;

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
            if (cloudStorage.equals(CloudStorageType.R_123)) {
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
        if (cloudStorage.equals(CloudStorageType.R_123)) {
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
        if (cloudStorage.equals(CloudStorageType.R_123)) {
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
            if (cloudStorage.equals(CloudStorageType.R_123)) {
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
        if (cloudStorage.equals(CloudStorageType.R_123)) {
            downloadUrl = r123Proxy.getDownloadUrl(ua, rileId);
        } else if (cloudStorage.equals(CloudStorageType.R_123_ZONG)) {
            downloadUrl = r123ZongProxy.getDownloadUrl(ua, rileId);
        } else if (cloudStorage.equals(R_115)) {
            Long copyToDir = getDeviceTmpDir(deviceId);
            ThreadUtil.execVirtual(() -> r115Proxy.copyFile(rileId, copyToDir));

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
     * 生成临时文件夹
     *
     * @param deviceId 设备标识符
     */
    public void mkdirDeviceTmpDir(String deviceId) {
        if (StrUtil.isBlank(deviceId)) {
            return;
        }
        Lock lock = LockUtil.lockDeviceLock(deviceId);
        if (LockUtil.isLock(lock)) {
            return;
        }
        try {
            getDeviceTmpDir(deviceId);
        } finally {
            LockUtil.unlockDevice(lock, deviceId);
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


    public static void main(String[] args) {
        String filePath = "//Users/mac/Library//CloudStorage\\OneDrive-%E4%B8%AA%E4%BA%BA%2Fworkspace%2Fgithub%2Fjava%2Ffasaxi-emma%2Fsrc%2Fmain%2Fjava%2Fcn%2Facecandy%2Ffasaxi%2Femma%2Futils%2FCloudUtil.java";
        Console.log(filePath);
        filePath = FileUtil.normalize(filePath);
        Console.log(filePath);
        Console.log(UrlPath.of(filePath, CharsetUtil.defaultCharset()).getSegments());
    }
}