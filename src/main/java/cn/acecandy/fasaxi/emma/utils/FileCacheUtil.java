package cn.acecandy.fasaxi.emma.utils;

import cn.acecandy.fasaxi.emma.common.ex.BaseException;
import cn.acecandy.fasaxi.emma.config.EmbyConfig;
import cn.acecandy.fasaxi.emma.config.EmbyContentCacheReqWrapper;
import cn.acecandy.fasaxi.emma.sao.out.EmbyItem;
import cn.acecandy.fasaxi.emma.sao.proxy.EmbyProxy;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.io.IoUtil;
import org.dromara.hutool.core.io.file.FileNameUtil;
import org.dromara.hutool.core.io.file.FileUtil;
import org.dromara.hutool.core.io.file.PathUtil;
import org.dromara.hutool.core.lang.Console;
import org.dromara.hutool.core.lang.tuple.Pair;
import org.dromara.hutool.core.map.MapUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.core.text.split.SplitUtil;
import org.dromara.hutool.crypto.SecureUtil;
import org.dromara.hutool.http.client.Request;
import org.dromara.hutool.http.client.Response;
import org.dromara.hutool.http.client.engine.ClientEngine;
import org.dromara.hutool.http.meta.HttpStatus;
import org.dromara.hutool.http.meta.Method;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_206;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_500;
import static cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType.电影;
import static cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType.电视剧;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;


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
    private ClientEngine httpClient;

    @Resource
    private EmbyProxy embyProxy;

    @Resource
    private EmbyUtil embyUtil;

    @Resource
    private EmbyConfig embyConfig;

    private static final String cache_path = "cache";
    private static final Map<String, ReentrantLock> FILE_CACHE_LOCK = MapUtil.newSafeConcurrentHashMap();
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
                    PathUtil.subPath(path, path.getNameCount() - 3, path.getNameCount()));
        } else {
            newFilePath.append("movie/").append(
                    PathUtil.subPath(path, path.getNameCount() - 2, path.getNameCount()));
        }
        return SecureUtil.md5(newFilePath.toString());
    }

    public static Path getCachePath(String itemId, String mediaType, String filePath) {
        return PathUtil.of(cache_path, mediaType, itemId,
                SecureUtil.md5(PathUtil.getLastPathEle(Paths.get(filePath)).toString()));
    }

    /**
     * 获取缓存路径
     *
     * @param embyItem emby项目
     * @return {@link Path }
     */
    public static Path getCachePath(EmbyItem embyItem) {
        String filePath = embyItem.getPath();
        String itemId = embyItem.getItemId();
        String mediaType = embyItem.getMediaType();
        return PathUtil.of(cache_path, mediaType, itemId,
                SecureUtil.md5(PathUtil.getLastPathEle(Paths.get(filePath)).toString()));
    }

    /**
     * 获取缓存完整路径
     *
     * @param embyItem emby项目
     * @return {@link Path }左边是根目录  右边是具体路径
     */
    public Pair<Path, Path> getCacheFullPath(EmbyItem embyItem, EmbyProxyUtil.Range range) {
        Path writePath = getCachePath(embyItem);
        String cacheFileName = StrUtil.format("cacheFile_{}_{}", range.start(), range.end());
        return Pair.of(Paths.get(embyConfig.getCachePath(), writePath.toString()),
                Paths.get(embyConfig.getCachePath(), writePath.toString(), cacheFileName));
    }

    public enum CacheStatus {
        UNKNOWN, MISS, PARTIAL, HIT, HIT_TAIL
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
        String realUrl = embyProxy.fetch302Path(path, reqHeader);

        String cacheFileName = "cacheFile_" + startPoint + "_" + endPoint;
        Path cacheFilePath = Paths.get(embyConfig.getCachePath(), subDir, dir, cacheFileName);
        Path parentPath = PathUtil.mkParentDirs(cacheFilePath);

        String cacheWriteTagPath = cacheFilePath + ".tag";
        FileUtil.touch(cacheWriteTagPath);

        ReentrantLock lock = getLock(subDir, dir);
        try {
            lock.lock();

            List<File> loopFile = PathUtil.loopFiles(parentPath, -1, null);
            long finalStartPoint = startPoint;
            long finalEndPoint = endPoint;
            loopFile.forEach(file -> {
                String fileName = FileNameUtil.getName(file);
                if (!StrUtil.startWith(fileName, "cacheFile_") || StrUtil.endWith(fileName, ".tag")) {
                    return;
                }
                List<String> parts = SplitUtil.split(fileName, "_");
                long fileStart = Long.parseLong(parts.get(1));
                long fileEnd = Long.parseLong(parts.get(2));
                if (finalStartPoint >= fileStart && finalEndPoint <= fileEnd) {
                    PathUtil.del(cacheFilePath);
                } else if (finalStartPoint <= fileStart && finalEndPoint >= fileEnd) {
                    FileUtil.del(file);
                }
            });
            if (reqHeader == null) {
                reqHeader = MapUtil.newHashMap();
            } else {
                reqHeader = new HashMap<>(reqHeader);
            }
            reqHeader.put("host", SplitUtil.split(realUrl, "/").get(2));
            reqHeader.put("range", StrUtil.format("bytes={}-{}", startPoint, endPoint));

            try (Response res = httpClient.send(Request.of(realUrl).header(reqHeader)).sync()) {
                if (res.getStatus() != HttpStatus.HTTP_PARTIAL) {
                    throw new BaseException("请求返回code不为206");
                }
                FileUtil.writeFromStream(res.body().getStream(), cacheFilePath.toFile());
                log.info("写入缓存文件成功[{}]: {}", itemId, cacheFilePath);
                FileUtil.del(cacheWriteTagPath);
                return true;
            }
        } catch (Exception e) {
            log.error("写入缓存失败[{}]: {}", itemId, cacheFilePath, e);
            PathUtil.del(cacheFilePath);
            FileUtil.del(cacheWriteTagPath);
            throw e;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 写入文件缓存
     *
     * @param request   要求
     * @param embyItem  emby项目
     * @param headerMap 请求头
     * @return boolean
     */
    @SneakyThrows
    public boolean writeFile(EmbyContentCacheReqWrapper request, EmbyItem embyItem,
                             Map<String, String> headerMap) {
        EmbyProxyUtil.Range range = EmbyProxyUtil.parseRangeHeader(request.getRange(), embyItem.getSize());
        if (null == range) {
            return false;
        }
        Pair<Path, Path> cacheFilePair = getCacheFullPath(embyItem, range);
        PathUtil.mkdir(cacheFilePair.getLeft());
        Path cacheFilePath = cacheFilePair.getRight();
        if (PathUtil.exists(cacheFilePath, false)) {
            log.info("缓存文件已存在: {}", cacheFilePath);
            return false;
        }
        log.info("写入本地缓存文件->{}", cacheFilePath);

        String mediaPath = CollUtil.getFirst(embyItem.getMediaSources()).getPath();
        // mediaPath = EmbyProxyUtil.getPtUrlOnHk(mediaPath);
        Request originalRequest = Request.of(mediaPath).method(Method.GET).header(headerMap).setMaxRedirects(1);
        try (Response res = httpClient.send(originalRequest);
             FileChannel fileChannel = FileChannel.open(cacheFilePath, CREATE, WRITE)) {
            fileChannel.position(range.start());
            ReadableByteChannel remoteChannel = Channels.newChannel(res.body().getStream());
            fileChannel.transferFrom(remoteChannel, 0, range.len());
        } catch (Exception e) {
            log.error("写入缓存文件失败: {}", cacheFilePath, e);
            PathUtil.del(cacheFilePath);
        }
        return true;
    }

    /**
     * 读取文件缓存
     *
     * @param embyItem emby项目
     * @param range    请求范围
     * @return boolean
     */
    @SneakyThrows
    public boolean readFile(HttpServletResponse response, EmbyItem embyItem, EmbyProxyUtil.Range range) {
        if (null == range) {
            return false;
        }
        Pair<Path, Path> cacheFilePair = getCacheFullPath(embyItem, range);
        Path cacheDir = cacheFilePair.getLeft();
        if (!PathUtil.isDirectory(cacheDir)) {
            return false;
        }
        // 2. 遍历所有缓存文件，匹配符合条件的范围
        long start = range.start();
        EmbyProxyUtil.CacheFile cacheFile = FileUtil.loopFiles(cacheDir.toFile()).stream()
                .map(file -> {
                    String[] parts = file.getName().split("_");
                    long cacheStart = Long.parseLong(parts[1]);
                    long cacheEnd = Long.parseLong(parts[2]);
                    return new EmbyProxyUtil.CacheFile(cacheStart, cacheEnd, file);
                })
                .filter(cf -> cf.start() <= start && cf.end() >= start)
                .max(Comparator.comparingLong(EmbyProxyUtil.CacheFile::end))
                .orElse(null);
        if (null == cacheFile) {
            return false;
        }
        // 从缓存文件名提取已缓存的范围
        String fileName = FileNameUtil.getName(cacheFile.file());

        // 计算实际可从缓存返回的范围
        long actualStart = start;
        long actualEnd = cacheFile.end();
        long actualLen = actualEnd - actualStart + 1;

        try (FileChannel inChannel = FileChannel.open(cacheFile.file().toPath(), StandardOpenOption.READ);
             ServletOutputStream out = response.getOutputStream()) {

            // 设置响应头（示例）
            response.setContentType(EmbyProxyUtil.getContentType(embyItem.getContainer()));
            response.setHeader("Content-Range",
                    StrUtil.format("bytes {}-{}/{}", actualStart, actualEnd, range.size()));
            response.setHeader("Accept-Ranges", "bytes");
            response.setContentLength((int) actualLen);
            response.setHeader("Cache-Control", "no-cache");
            response.setStatus(CODE_206);

            log.warn("开始读取缓存文件:{} => {}", range.toHeader(), fileName);
            long offset = actualStart - cacheFile.start();
            long transferred = inChannel.transferTo(offset, actualLen, Channels.newChannel(out));
            // 7. 完整性验证（新增）
            if (transferred != actualLen) {
                log.warn("传输不完整，offset:{}, 预期:{}, 实际:{}", offset, actualLen, transferred);
                return false;
            }
            return true;
        } catch (ClientAbortException e) {
            log.info("客户端主动中断下载: {}", cacheFile.file().getName());
            return true;
        } catch (IOException e) {
            log.error("读取缓存文件失败: {}", fileName, e);
            response.setStatus(CODE_500);
            return false;
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

        Path fileDir = Paths.get(embyConfig.getCachePath(), subDir, dir);
        for (File file : PathUtil.loopFiles(fileDir, -1, null)) {
            String fileName = FileNameUtil.getName(file);
            if (!StrUtil.startWith(fileName, "cacheFile_") || StrUtil.endWith(fileName, ".tag")) {
                continue;
            }
            List<String> parts = SplitUtil.split(fileName, "_");
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

        Path fileDir = PathUtil.of(embyConfig.getCachePath(), subDir, dir);
        for (File file : PathUtil.loopFiles(fileDir, -1, null)) {
            String fileName = FileNameUtil.getName(file);
            if (!StrUtil.startWith(fileName, "cacheFile_") || StrUtil.endWith(fileName, ".tag")) {
                continue;
            }
            List<String> parts = SplitUtil.split(fileName, "_");
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

        Path fileDir = Paths.get(embyConfig.getCachePath(), subDir, dir);
        if (FileUtil.isEmpty(fileDir.toFile())) {
            return false;
        }

        // 检查是否有任何缓存文件正在写入
        List<File> files = FileUtil.loopFiles(fileDir.toFile());
        if (files.stream().map(FileNameUtil::getName)
                .anyMatch(fileName -> StrUtil.endWith(fileName, ".tag"))) {
            return false;
        }

        // 这时候已经没有tag结尾的文件了 查找与 startPoint 匹配的缓存文件，endPoint 为文件名的一部分
        for (File file : files) {
            String fileName = FileNameUtil.getName(file);
            if (!StrUtil.startWith(fileName, "cacheFile_")) {
                continue;
            }
            List<String> parts = SplitUtil.split(fileName, "_");
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

        File cacheDir = FileUtil.file(embyConfig.getCachePath(), subDir, dir);
        ReentrantLock lock = getLock(subDir, dir);

        if (FileUtil.isEmpty(cacheDir) || !FileUtil.isDirectory(cacheDir)) {
            return false;
        }

        FileUtil.loopFiles(cacheDir).stream().filter(f ->
                StrUtil.startWith(FileNameUtil.getName(f), "cacheFile_")).forEach(FileUtil::del);
        if (CollUtil.isEmpty(FileUtil.loopFiles(cacheDir))) {
            FileUtil.del(cacheDir);
            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        Console.log(getCachePath("12345", 电影.getValue(), "/vol2/1000/dockerThirdConf/bili-sync-rs/docker-compose.yml"));
    }
}