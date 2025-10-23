package cn.acecandy.fasaxi.emma.utils;

import cn.hutool.v7.core.convert.ConvertUtil;
import cn.hutool.v7.core.math.NumberUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.core.util.ByteUtil;
import cn.hutool.v7.http.client.Request;
import cn.hutool.v7.http.client.Response;
import cn.hutool.v7.http.client.engine.ClientEngine;
import cn.hutool.v7.http.meta.HeaderName;
import cn.hutool.v7.http.meta.Method;
import jakarta.annotation.Resource;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 视频流分析工具类
 *
 * @author AceCandy
 * @since 2025/5/17
 */
@Slf4j
@Component
public class VideoUtil {

    /**
     * MP4 BOX类型常量
     */
    private static final byte[] BOX_TYPE_MOOV = ByteUtil.toUtf8Bytes("moov");
    private static final byte[] BOX_TYPE_MDAT = ByteUtil.toUtf8Bytes("mdat");
    private static final String 头部 = "头部";
    private static final String 尾部 = "尾部";
    private static final String mdat之后 = "mdat之后";
    @Resource
    private ClientEngine httpClient;

    public static void main(String[] args) {
        /*try {
            // 示例：分析指定URL的MP4视频
            String videoUrl = "http://alist.netcup-1o2.worldline.space/p/pt/Emby1/%E5%9B%BD%E4%BA%A7%E5%89%A7/%E5%AD%A4%E5%B2%9B%E9%A3%9E%E9%B9%B0%20%282012%29/Season%201/%E5%AD%A4%E5%B2%9B%E9%A3%9E%E9%B9%B0%20-%20S01E21%20-%20%E7%AC%AC21%E9%9B%86%20-%20QHStudIo.mp4";
            VideoUtil analyzer = new VideoUtil();

            // 方法1：直接从HTTP请求分析（推荐）
            MoovPos position = analyzer.analyzeMp4(videoUrl, 0L);
            if (position != null) {
                System.out.println("找到moov BOX:");
                System.out.println("  起始位置: " + position.getStart() + " 字节");
                System.out.println("  大小: " + position.getSize() + " 字节");
                System.out.println("  相对位置: " + position.getPosDesc());
            } else {
                System.out.println("未找到moov BOX，可能需要扩展搜索范围或使用其他方法。");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    /**
     * 核心Box查找逻辑（支持偏移校正）
     *
     * @param data       数据
     * @param baseOffset 基本偏移
     * @return {@link MoovPos }
     */
    private static MoovPos findMoovInData(byte[] data, long baseOffset) {
        BoxInfo box = findBox(data, BOX_TYPE_MOOV);
        return box != null ? MoovPos.builder().start(box.start() + baseOffset)
                .size(box.size()).build() : null;
    }

    /**
     * 通用Box查找方法
     *
     * @param data       数据
     * @param targetType 目标类型
     * @return {@link BoxInfo }
     */
    private static BoxInfo findBox(byte[] data, byte[] targetType) {
        long offset = 0;
        // 保证至少有8字节头部
        while (offset <= data.length - 8) {
            final int pos = (int) offset;

            // 读取头部信息
            long size = readUnInt32(data, pos);
            byte[] type = Arrays.copyOfRange(data, pos + 4, pos + 8);

            // 处理扩展尺寸
            if (size == 1 && offset + 16 <= data.length) {
                size = readUnInt64(data, pos + 8);
                // 跳过扩展头
                offset += 16;
            } else {
                // 标准头
                offset += 8;
            }

            // 类型匹配检测
            if (Arrays.equals(type, targetType)) {
                // 回退到实际起始位置
                return new BoxInfo(offset - 8, size);
            }

            // 处理特殊尺寸
            // 剩余部分为当前box
            if (size == 0) size = data.length - offset + 8;
            // 尺寸无效
            if (size < 8 || offset + (size - 8) > data.length) break;

            // 移动到下一box
            offset += (size - 8);
        }
        return null;
    }

    /**
     * 实用方法：读取32位无符号整数
     *
     * @param data   数据
     * @param offset 抵消
     * @return long
     */
    private static long readUnInt32(byte[] data, int offset) {
        return (data[offset] & 0xFFL) << 24 | (data[offset + 1] & 0xFFL) << 16 |
                (data[offset + 2] & 0xFFL) << 8 | (data[offset + 3] & 0xFFL);
    }

    /**
     * 实用方法：读取64位无符号整数
     *
     * @param data   数据
     * @param offset 抵消
     * @return long
     */
    private static long readUnInt64(byte[] data, int offset) {
        return (data[offset] & 0xFFL) << 56 | (data[offset + 1] & 0xFFL) << 48 |
                (data[offset + 2] & 0xFFL) << 40 | (data[offset + 3] & 0xFFL) << 32 |
                (data[offset + 4] & 0xFFL) << 24 | (data[offset + 5] & 0xFFL) << 16 |
                (data[offset + 6] & 0xFFL) << 8 | (data[offset + 7] & 0xFFL);
    }

    /**
     * 从URL分析moov BOX位置，自动处理优化和未优化的MP4文件
     */
    public MoovPos analyzeMp4(String url, Long fileSize) {
        // 1. 先尝试获取文件总大小
        if (NumberUtil.isValidNumber(fileSize) || NumberUtil.isZero(fileSize)) {
            fileSize = getFileSize(url);
        }

        // 2. 分析文件头部前8k（适用于优化过的MP4）
        byte[] headData = downloadRange(url, 0, 8 * 1024 - 1);
        MoovPos pos = findMoovInData(headData, 0);
        if (pos != null) return pos.withPosDesc(头部);

        // 3. 分析尾部1m（适用于未优化的MP4）
        if (fileSize > 0) {
            long tailStart = Math.max(0, fileSize - 1024 * 1024);
            byte[] tailData = downloadRange(url, tailStart, fileSize - 1);
            pos = findMoovInData(tailData, tailStart);
            if (pos != null) return pos.withPosDesc(尾部);
        }

        // 4. 尝试结合mdat位置推测moov位置
        BoxInfo mdat = findBox(headData, BOX_TYPE_MDAT);
        if (mdat != null && fileSize > 0) {
            long searchStart = mdat.start() + mdat.size();
            long searchEnd = Math.min(fileSize - 1, searchStart + 1024 * 1024);
            byte[] searchData = downloadRange(url, searchStart, searchEnd);
            pos = findMoovInData(searchData, searchStart);
            if (pos != null) return pos.withPosDesc(mdat之后);
        }
        log.warn("未找到moov位置,size:{} -> {}", fileSize, url);
        return null;
    }

    /**
     * 下载指定范围的视频数据
     */
    @SneakyThrows
    private byte[] downloadRange(String url, long start, long end) {
        try (Response resp = httpClient.send(Request.of(url).method(Method.GET)
                .header("Range", "bytes=" + start + "-" + end).setMaxRedirects(1))) {
            return resp.bodyBytes();
        }
    }

    /**
     * 获取文件总大小
     */
    @SneakyThrows
    private long getFileSize(String url) {
        try (Response resp = httpClient.send(Request.of(url).method(Method.HEAD).setMaxRedirects(1))) {
            return ConvertUtil.toLong(resp.header(HeaderName.CONTENT_LENGTH), -1L);
        }
    }

    /**
     * 对外暴露的Moov位置信息
     */
    @Data
    @Builder
    public static class MoovPos {
        private long start;
        private long size;
        private String posDesc;

        public MoovPos withPosDesc(String posDesc) {
            this.posDesc = posDesc;
            return this;
        }

        public String toHead() {
            return StrUtil.format("bytes={}-{}", start, size + start - 1);
        }
    }

    /**
     * 内部Box信息类
     */
    private record BoxInfo(long start, long size) {
    }

}