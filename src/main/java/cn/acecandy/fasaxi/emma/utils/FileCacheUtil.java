package cn.acecandy.fasaxi.emma.utils;

import cn.acecandy.fasaxi.emma.common.ex.BaseException;
import cn.acecandy.fasaxi.emma.config.FastEmbyConfig;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.PathUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import static cn.acecandy.fasaxi.emma.utils.FileCacheUtil.EmbyMediaType.电影;
import static cn.acecandy.fasaxi.emma.utils.FileCacheUtil.EmbyMediaType.电视剧;

/**
 * emby 文件缓存 工具类
 *
 * @author tangningzhu
 * @since 2024/10/16
 */
@Slf4j
@Component
public class FileCacheUtil {

    @Resource
    private CloseableHttpClient embyHttpClient;

    @Resource
    private EmbyUtil embyUtil;

    @Resource
    private FastEmbyConfig feConfig;

    private static final String cache_path = "cache";
    private static final Map<String, ReentrantLock> FILE_CACHE_LOCK = MapUtil.newConcurrentHashMap();
    // private static final OkHttpClient client = new OkHttpClient();

    private static ReentrantLock getLock(String subdirname, String dirname) {
        String key = subdirname + "/" + dirname;
        return FILE_CACHE_LOCK.computeIfAbsent(key, k -> new ReentrantLock());
    }

    /**
     * 获取路径的md5值
     * <p>
     * 电视剧取后三层目录 电影取后两层
     * md5前两位可用作缓存目录文件夹名
     *
     * @param filePath  文件路径
     * @param mediaType 纸张类型
     * @return {@link String }
     */
    public static String getPathMd5(String filePath, String mediaType) {
        Path path = Paths.get(filePath);
        StringBuilder newFilePath = new StringBuilder();
        if (!电影.getValue().equals(mediaType)) {
            newFilePath.append("series/").append(
                    FileUtil.subPath(path, path.getNameCount() - 3, path.getNameCount()));
        } else {
            newFilePath.append("movie/").append(
                    FileUtil.subPath(path, path.getNameCount() - 2, path.getNameCount()));
        }
        return SecureUtil.md5(newFilePath.toString());
    }

    public enum CacheStatus {
        UNKNOWN, MISS, PARTIAL, HIT, HIT_TAIL
    }

    @AllArgsConstructor
    @Getter
    enum EmbyMediaType {
        // 接口出的类型
        电影("Movie"),
        电视剧("Episode"),
        ;

        private final String value;
    }

    @Builder
    @Data
    static class RequestInfo {
        private FileInfo fileInfo;
        private CacheStatus cacheStatus;

        private String rawUrl;
        private String hostUrl;
        private CompletableFuture<String> rawUrlTask;
        private Map<String, String> headers;
    }

    @Data
    @Builder
    public static class FileInfo {
        /**
         * alist路径
         */
        private String path;
        /**
         * 真实URL
         */
        private String realUrl;
        /**
         * 类型 Movie/Episode
         */
        private String itemType;
        /**
         * 项目id
         */
        private Long itemId;
        /**
         * 季id
         */
        private Long seasonId;
        private Long bitrate;
        private Long size;
        private String container;
        private Long cacheFileSize;

        /**
         * 缓存参数
         */
        private Long startByte;
        private Long endByte;
        private CacheStatus cacheStatus;
    }

    /**
     * 写入文件缓存
     *
     * @param fileInfo  请求信息
     * @param reqHeader 请求入参头球
     * @return boolean
     */
    @SneakyThrows
    public boolean writeCacheFile(FileInfo fileInfo,
                                  Map<String, String> reqHeader) {
        log.info("写入本地缓存文件->");
        Long itemId = fileInfo.getItemId();
        String path = fileInfo.getPath();
        long fileSize = fileInfo.getSize();
        long cacheSize = fileInfo.getCacheFileSize();
        String itemType = fileInfo.getItemType();

        String pathMd5 = getPathMd5(path, itemType);
        String subDir = StrUtil.subPre(pathMd5, 2);
        String dir = pathMd5;

        Long startPoint = fileInfo.getStartByte();
        Long endPoint = fileInfo.getEndByte();
        if (null == endPoint) {
            endPoint = Math.min(fileSize, startPoint + cacheSize) - 1;
        }
        /*switch (fileInfo.getCacheStatus()) {
            case PARTIAL, HIT -> {
                startPoint = 0;
                endPoint = cacheSize - 1;
            }
            case HIT_TAIL -> {
                startPoint = fileInfo.getStartByte();
                endPoint = fileSize - 1;
            }
            default -> throw new BaseException("未知的缓存状态");
        }*/
        String realUrl = embyUtil.fetch302Path(path, reqHeader);

        String cacheFileName = "cacheFile_" + startPoint + "_" + endPoint;
        Path cacheFilePath = Paths.get(feConfig.getCachePath(), subDir, dir, cacheFileName);
        Path parentPath = FileUtil.mkParentDirs(cacheFilePath);

        String cacheWriteTagPath = cacheFilePath + ".tag";
        FileUtil.touch(cacheWriteTagPath);

        ReentrantLock lock = getLock(subDir, dir);
        try {
            lock.lock();

            List<File> loopFile = PathUtil.loopFiles(parentPath, -1, null);
            long finalStartPoint = startPoint;
            long finalEndPoint = endPoint;
            loopFile.forEach(file -> {
                String fileName = FileUtil.getName(file);
                if (!StrUtil.startWith(fileName, "cacheFile_") || StrUtil.endWith(fileName, ".tag")) {
                    return;
                }
                List<String> parts = StrUtil.split(fileName, "_");
                long fileStart = Long.parseLong(parts.get(1));
                long fileEnd = Long.parseLong(parts.get(2));
                if (finalStartPoint >= fileStart && finalEndPoint <= fileEnd) {
                    FileUtil.del(cacheFilePath);
                } else if (finalStartPoint <= fileStart && finalEndPoint >= fileEnd) {
                    FileUtil.del(file);
                }
            });
            if (reqHeader == null) {
                reqHeader = MapUtil.newHashMap();
            } else {
                reqHeader = new HashMap<>(reqHeader);
            }
            reqHeader.put("host", StrUtil.split(realUrl, "/").get(2));
            reqHeader.put("range", StrUtil.format("bytes={}-{}", startPoint, endPoint));

            HttpUriRequest req = RequestBuilder.get(realUrl).build();
            reqHeader.forEach(req::addHeader);
            try (CloseableHttpResponse resp = embyHttpClient.execute(req)) {
                if (resp.getStatusLine().getStatusCode() != 206) {
                    throw new BaseException("请求返回code不为206");
                }
                FileUtil.writeFromStream(resp.getEntity().getContent(), cacheFilePath.toFile());
                log.info("写入缓存文件成功[{}]: {}", itemId, cacheFilePath);
                FileUtil.del(cacheWriteTagPath);
                return true;
            }
        } catch (Exception e) {
            log.error("写入缓存失败[{}]: {}", itemId, cacheFilePath, e);
            FileUtil.del(cacheFilePath);
            FileUtil.del(cacheWriteTagPath);
            throw e;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 读取缓存文件
     *
     * @param fileInfo 请求信息
     * @return {@link Stream }<{@link byte[] }>
     */
    public Stream<byte[]> readCacheFile(FileInfo fileInfo) {
        String path = fileInfo.getPath();

        String pathMd5 = getPathMd5(path, fileInfo.getItemType());
        String subDir = StrUtil.subPre(pathMd5, 2);
        String dir = pathMd5;

        Path fileDir = Paths.get(feConfig.getCachePath(), subDir, dir);
        for (File file : FileUtil.loopFiles(fileDir, -1, null)) {
            String fileName = FileUtil.getName(file);
            if (!StrUtil.startWith(fileName, "cacheFile_") || StrUtil.endWith(fileName, ".tag")) {
                continue;
            }
            List<String> parts = StrUtil.split(fileName, "_");
            long fileStart = Long.parseLong(parts.get(1));
            long fileEnd = Long.parseLong(parts.get(2));
            if (fileStart <= fileInfo.getStartByte() && fileInfo.getStartByte() <= fileEnd) {
                Long adjustedEnd = null;
                if (fileInfo.getCacheStatus() == CacheStatus.HIT) {
                    adjustedEnd = fileInfo.getEndByte() - fileInfo.getStartByte();
                }
                log.info("读取缓存文件: {}", file.getAbsolutePath());
                return new FileRangeReader(FileUtil.getAbsolutePath(file),
                        fileInfo.getStartByte(), adjustedEnd, 1024 * 1024).stream();
            }
        }
        return Stream.empty();
    }

    public InputStream readCacheFileInput(FileInfo fileInfo) {
        String path = fileInfo.getPath();

        String pathMd5 = getPathMd5(path, fileInfo.getItemType());
        String subDir = StrUtil.subPre(pathMd5, 2);
        String dir = pathMd5;

        Path fileDir = Paths.get(feConfig.getCachePath(), subDir, dir);
        for (File file : FileUtil.loopFiles(fileDir, -1, null)) {
            String fileName = FileUtil.getName(file);
            if (!StrUtil.startWith(fileName, "cacheFile_") || StrUtil.endWith(fileName, ".tag")) {
                continue;
            }
            List<String> parts = StrUtil.split(fileName, "_");
            long fileStart = Long.parseLong(parts.get(1));
            long fileEnd = Long.parseLong(parts.get(2));
            if (fileStart <= fileInfo.getStartByte() && fileInfo.getStartByte() <= fileEnd) {
                Long adjustedEnd = null;
                if (fileInfo.getCacheStatus() == CacheStatus.HIT) {
                    adjustedEnd = fileInfo.getEndByte() - fileInfo.getStartByte();
                }
                return IoUtil.toStream(file);
            }
        }
        return null;
    }

    /**
     * 检查缓存文件是否存在
     *
     * @param fileInfo 请求信息
     * @return boolean
     */
    public boolean checkCacheFile(FileInfo fileInfo) {
        String path = fileInfo.getPath();

        String pathMd5 = getPathMd5(path, fileInfo.getItemType());
        String subDir = StrUtil.subPre(pathMd5, 2);
        String dir = pathMd5;

        Path fileDir = Paths.get(feConfig.getCachePath(), subDir, dir);
        if (!FileUtil.exist(fileDir.toFile())) {
            return false;
        }

        // 检查是否有任何缓存文件正在写入
        List<File> files = FileUtil.loopFiles(fileDir.toFile());
        if (files.stream().map(FileUtil::getName)
                .anyMatch(fileName -> StrUtil.endWith(fileName, ".tag"))) {
            return false;
        }

        // 这时候已经没有tag结尾的文件了 查找与 startPoint 匹配的缓存文件，endPoint 为文件名的一部分
        for (File file : files) {
            String fileName = FileUtil.getName(file);
            if (!StrUtil.startWith(fileName, "cacheFile_")) {
                continue;
            }
            List<String> parts = StrUtil.split(fileName, "_");
            long fileStart = Long.parseLong(parts.get(1));
            long fileEnd = Long.parseLong(parts.get(2));
            if (fileStart <= fileInfo.getStartByte() && fileInfo.getStartByte() <= fileEnd) {
                return true;
            }
            /*if (verifyCacheFile(fileInfo, CollUtil.newArrayList(fileStart, fileEnd))) {
                if (fileStart <= fileInfo.getStartByte() && fileInfo.getStartByte() <= fileEnd) {
                    return true;
                }
            } else {
                FileUtil.del(file);
                continue;
            }*/
        }
        return false;
    }

    /**
     * 缓存下一集
     * <p>
     * 如果是剧集则缓存下一集；如果是电影则跳过
     *
     * @param fileInfo 请求信息
     * @param header   请求头
     * @return {@link CompletableFuture }<{@link Boolean }>
     */
    public Boolean cacheNextEpisode(FileInfo fileInfo, Map<String, String> header) {
        // 不是剧集就跳过
        if (!StrUtil.equals(电视剧.getValue(), fileInfo.getItemType())) {
            return false;
        }

        long nextId = fileInfo.getItemId() + 1;
        FileInfo nextFileInfo = embyUtil.getFileInfo(nextId);
        // 无下一集或者下一集不是同一季 不进行缓存
        if (null == nextFileInfo || !nextFileInfo.getSeasonId().equals(fileInfo.getSeasonId())) {
            return false;
        }
        nextFileInfo.setCacheStatus(CacheStatus.PARTIAL);
        nextFileInfo.setStartByte(0L);
        nextFileInfo.setEndByte(null);
        if (checkCacheFile(nextFileInfo)) {
            return false;
        } else {
            return writeCacheFile(nextFileInfo, header);
        }
    }

    /**
     * 验证缓存文件是否符合 Emby 文件大小，筛选出错误缓存文件
     * <p>
     * 实现方式仅为验证文件大小，不验证文件内容
     *
     * @param fileInfo       文件简介
     * @param cacheFileRange 缓存文件范围
     * @return boolean
     */
    public static boolean verifyCacheFile(FileInfo fileInfo, List<Long> cacheFileRange) {
        long start = CollUtil.getFirst(cacheFileRange);
        long end = CollUtil.getLast(cacheFileRange);

        // 开头缓存文件
        boolean isStartCache = start == 0 && end == fileInfo.cacheFileSize - 1;
        boolean isEndCache = end == fileInfo.size - 1;
        return isStartCache || isEndCache;
    }

    /**
     * 根据webhook信息删除缓存文件，及缓存文件夹
     *
     * @param fileInfo 文件简介
     * @return boolean
     */
    public boolean cleanCache(FileInfo fileInfo) {
        String path = fileInfo.getPath();
        String pathMd5 = getPathMd5(path, fileInfo.getItemType());
        String subDir = StrUtil.subPre(pathMd5, 2);
        String dir = pathMd5;

        File cacheDir = FileUtil.file(feConfig.getCachePath(), subDir, dir);
        ReentrantLock lock = getLock(subDir, dir);

        if (!FileUtil.exist(cacheDir) || !FileUtil.isDirectory(cacheDir)) {
            return false;
        }

        FileUtil.loopFiles(cacheDir).stream().filter(f ->
                StrUtil.startWith(FileUtil.getName(f), "cacheFile_")).map(FileUtil::del);
        if (CollUtil.isEmpty(FileUtil.loopFiles(cacheDir))) {
            return FileUtil.del(cacheDir);
        }
        return false;
    }
}