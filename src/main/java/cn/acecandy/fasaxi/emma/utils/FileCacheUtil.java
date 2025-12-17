package cn.acecandy.fasaxi.emma.utils;

import cn.acecandy.fasaxi.emma.config.EmbyConfig;
import cn.acecandy.fasaxi.emma.sao.out.EmbyItem;
import cn.acecandy.fasaxi.emma.sao.proxy.EmbyProxy;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.io.file.FileNameUtil;
import cn.hutool.v7.core.io.file.FileUtil;
import cn.hutool.v7.core.io.file.PathUtil;
import cn.hutool.v7.core.lang.Console;
import cn.hutool.v7.core.lang.tuple.Pair;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.crypto.SecureUtil;
import cn.hutool.v7.http.client.Request;
import cn.hutool.v7.http.client.Response;
import cn.hutool.v7.http.client.engine.ClientEngine;
import cn.hutool.v7.http.meta.Method;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_206;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_500;
import static cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType.电影;
import static cn.acecandy.fasaxi.emma.common.enums.EmbyMediaType.电视剧_集;
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
    private EmbyConfig embyConfig;
    @Resource
    private VideoUtil videoUtil;

    public static Path getCachePath(String itemId, String mediaType, String filePath) {
        return PathUtil.of(mediaType, itemId,
                SecureUtil.md5(PathUtil.getLastPathEle(Paths.get(filePath)).toString()));
    }

    public static void main(String[] args) {
        Console.log(getCachePath("12345", 电影.getEmbyName(), "/vol2/1000/dockerThirdConf/bili-sync-rs/docker-compose.yml"));
    }

    /**
     * 获取缓存路径
     *
     * @param embyItem emby项目
     * @return {@link Path }
     */
    public Path getCacheDir(EmbyItem embyItem) {
        String filePath = embyItem.getPath();
        String itemId = embyItem.getItemId();
        String type = embyItem.getType();
        String md5FileName = SecureUtil.md5(PathUtil.getLastPathEle(Paths.get(filePath)).toString());

        if (电视剧_集.getEmbyName().equals(type)) {
            return PathUtil.of(embyConfig.getCachePath(), type,
                    embyItem.getSeriesId(), embyItem.getSeasonId(), itemId, md5FileName);
        }
        return PathUtil.of(embyConfig.getCachePath(), type, itemId, md5FileName);
    }

    /**
     * 获取缓存完整路径
     *
     * @param embyItem emby项目
     * @return {@link Path }左边是根目录  右边是具体路径
     */
    public Pair<Path, Path> getCacheFullPath(EmbyItem embyItem, EmbyProxyUtil.Range range) {
        Path writePath = getCacheDir(embyItem);
        String cacheFileName = StrUtil.format("cacheFile_{}_{}", range.start(), range.end());
        return Pair.of(Paths.get(writePath.toString()),
                Paths.get(writePath.toString(), cacheFileName));
    }

    /**
     * 获取缓存完整路径
     *
     * @param embyItem emby项目
     * @return {@link Path }左边是根目录  右边是具体路径
     */
    public Pair<Path, Path> getMoovFullPath(EmbyItem embyItem, EmbyProxyUtil.Range range) {
        Path writePath = getCacheDir(embyItem);
        String cacheFileName = StrUtil.format("moov_{}_{}", range.start(), range.end());
        return Pair.of(Paths.get(writePath.toString()),
                Paths.get(embyConfig.getCachePath(), writePath.toString(), cacheFileName));
    }

    /**
     * 写入文件缓存
     *
     * @param embyItem emby项目
     * @return boolean
     */
    @SneakyThrows
    public boolean writeFile(EmbyItem embyItem) {
        Map<String, String> headerMap = null;
        // EmbyProxyUtil.Range range = EmbyProxyUtil.parseRangeHeader(request.getRange(), embyItem.getSize());
        Long size = embyItem.getSize();
        if (null == size || size == 0) {
            return false;
        }
        long cacheSize = Math.max(size / (embyItem.getRunTimeTicks() / 10000 / 60 / 1000), 1024 * 1024 * 10);
        if (size < cacheSize) {
            return false;
        }
        EmbyProxyUtil.Range range = new EmbyProxyUtil.Range(0, cacheSize, embyItem.getSize());
        Pair<Path, Path> cacheFilePair = getCacheFullPath(embyItem, range);
        PathUtil.mkdir(cacheFilePair.getLeft());
        Path cacheFilePath = cacheFilePair.getRight();
        if (PathUtil.exists(cacheFilePath, false)) {
            log.debug("缓存文件已存在: {}", cacheFilePath);
            return true;
        }
        log.info("准备写入本地缓存文件->{}", cacheFilePath);
        File tmpTag = FileUtil.touch(cacheFilePair.getRight().toString() + ".tag");
        String mediaPath = CollUtil.getFirst(embyItem.getMediaSources()).getPath();
        mediaPath = EmbyProxyUtil.getPtUrlOnHk(mediaPath);
        Request originalRequest = Request.of(mediaPath).method(Method.GET)
                .header("Range", range.toHeader()).setMaxRedirects(1);
        try (Response res = httpClient.send(originalRequest);
             FileChannel fileChannel = FileChannel.open(tmpTag.toPath(), CREATE, WRITE)) {
            if (!res.isOk()) {
                throw new RuntimeException(StrUtil.format("下载视频失败: [{}]{}",
                        range.toHeader(), mediaPath));
            }
            Console.log(res.getStatus());
            fileChannel.position(range.start());
            ReadableByteChannel remoteChannel = Channels.newChannel(res.body().getStream());
            fileChannel.transferFrom(remoteChannel, 0, range.len());
            FileUtil.move(tmpTag, cacheFilePath.toFile(), true);
            log.warn("写入本地缓存文件完成->{}", cacheFilePath);
        } catch (Exception e) {
            log.error("写入缓存文件失败: {}", cacheFilePath, e);
            FileUtil.del(tmpTag);
            PathUtil.del(cacheFilePath);
        }
        return true;
    }

    @SneakyThrows
    public boolean writeMoovFile(EmbyItem embyItem) {
        Path writePath = getCacheDir(embyItem);
        if (FileUtil.listFileNames(writePath.toString()).stream()
                .anyMatch(name -> StrUtil.startWith(name, "moov"))) {
            log.debug("moov文件已存在: {}", writePath);
            return false;
        }
        String mediaPath = CollUtil.getFirst(embyItem.getMediaSources()).getPath();
        mediaPath = EmbyProxyUtil.getPtUrlOnHk(mediaPath);
        VideoUtil.MoovPos moovPos = switch (embyItem.getContainer()) {
            case "mp4", "mov", "3gp" -> videoUtil.analyzeMp4(mediaPath, embyItem.getSize());
            default -> VideoUtil.MoovPos.builder().start(embyItem.getSize() - 512 * 1024)
                    .size(512 * 1024).posDesc("虚拟").build();
        };
        if (null == moovPos) {
            return false;
        }
        int adjustment = 16 * 1024;
        long newStart = Math.max(0, moovPos.getStart() - adjustment);
        // size需要补偿 start 被截断的部分
        long sizeIncrease = adjustment + (moovPos.getStart() - newStart);

        moovPos.setStart(newStart);
        moovPos.setSize(moovPos.getSize() + sizeIncrease);
        long moovEnd = moovPos.getStart() + moovPos.getSize() - 1;

        boolean cacheContainMoov = FileUtil.loopFiles(writePath.toFile()).stream().filter(file -> {
            String fileName = FileNameUtil.getName(file);
            return StrUtil.startWithAny(fileName, "cacheFile_") && !StrUtil.endWith(fileName, ".tag");
        }).map(file -> {
            String[] parts = file.getName().split("_");
            long cacheStart = Long.parseLong(parts[1]);
            long cacheEnd = Long.parseLong(parts[2]);
            return new EmbyProxyUtil.CacheFile(cacheStart, cacheEnd, file);
        }).anyMatch(cf -> cf.start() <= moovPos.getStart() && cf.end() >= moovEnd);
        if (cacheContainMoov) {
            log.info("moov文件已存在cache子集中: {}", writePath);
            return false;
        }


        log.info("找到moov位置，准备写入: {}", moovPos);
        File tmpTag = FileUtil.touch(writePath.toFile(), "moov.tag");

        Request originalRequest = Request.of(mediaPath).method(Method.GET)
                .header("Range", moovPos.toHead()).setMaxRedirects(1);
        try (Response res = httpClient.send(originalRequest);
             FileChannel fileChannel = FileChannel.open(tmpTag.toPath(), CREATE, WRITE)) {
            if (!res.isOk()) {
                throw new RuntimeException(StrUtil.format("下载视频失败: [{}]{}",
                        moovPos.toHead(), mediaPath));
            }
            fileChannel.position(moovPos.getStart());
            ReadableByteChannel remoteChannel = Channels.newChannel(res.body().getStream());
            fileChannel.transferFrom(remoteChannel, 0, moovPos.getSize());
            String moovName = StrUtil.format("moov{}_{}_{}",
                    StrUtil.equals(moovPos.getPosDesc(), "虚拟") ? "X" : "",
                    moovPos.getStart(), moovEnd);
            tmpTag = FileUtil.rename(tmpTag, moovName, true);
            log.warn("写入本地moov文件完成->{}", moovName);
        } catch (Exception e) {
            log.error("写入moov文件失败: {}", FileNameUtil.getName(tmpTag), e);
            FileUtil.del(tmpTag);
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
    public boolean readFile(HttpServletResponse response,
                            EmbyItem embyItem, EmbyProxyUtil.Range range) {
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
                .filter(file -> {
                    String fileName = FileNameUtil.getName(file);
                    return StrUtil.startWithAny(fileName, "cacheFile_", "moov") && !StrUtil.endWith(fileName, ".tag");
                })
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

        /*response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", "https://alist.acecandy.cn:880/d/local-vol2" +
                StrUtil.removePrefix(cacheFilePair.getRight().toString(),"/vol2/1000"));
        return true;*/

        try (FileChannel inChannel = FileChannel.open(cacheFile.file().toPath(), StandardOpenOption.READ);
             ServletOutputStream out = response.getOutputStream()) {

            // FileChannel inChannel = FileChannel.open(cacheFile.file().toPath(), StandardOpenOption.READ);
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
        } catch (ClosedChannelException ce) {
            log.info("客户端关闭，无须写入: {}", cacheFile.file().getName());
            return true;
        } catch (IOException e) {
            if (ExceptUtil.isConnectionTerminated(e)) {
                log.info("客户端主动中断下载: {}", cacheFile.file().getName());
                return true;
            } else {
                log.error("读取缓存文件失败: {}", fileName, e);
                response.setStatus(CODE_500);
                return false;
            }
        }
    }

    /**
     * 缓存下一集
     * <p>
     * 如果是剧集则缓存下一集；如果是电影则跳过
     *
     * @param embyItem emby项目
     */
    public void cacheNextEpisode(EmbyItem embyItem) {
        // 不是剧集就跳过
        if (!StrUtil.equals(电视剧_集.getEmbyName(), embyItem.getType())) {
            return;
        }
        List<EmbyItem> seasonItem = embyProxy.getEpisodes(embyItem.getItemId(), embyItem.getSeasonId());
        int index = -1;
        for (int i = 0; i < seasonItem.size(); i++) {
            if (seasonItem.get(i).getItemId().equals(embyItem.getItemId())) {
                index = i;
                break;
            }
        }
        if (index == -1 || index == seasonItem.size() - 1) {
            // 没有找到符合条件或者已经最后一集了
            return;
        }
        EmbyItem nextItem = seasonItem.get(index + 1);
        writeCacheAndMoov(nextItem);
    }

    public void writeCacheAndMoov(EmbyItem embyItem) {
        ReentrantLock lock = LockUtil.lockVideoCache(embyItem.getItemId());
        if (LockUtil.isLock(lock)) {
            return;
        }
        try {
            if (writeFile(embyItem)) {
                writeMoovFile(embyItem);
            }
        } finally {
            LockUtil.unlockVideoCache(lock, embyItem.getItemId());
        }
    }
}