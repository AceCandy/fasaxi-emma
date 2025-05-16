package cn.acecandy.fasaxi.emma.utils;

import org.dromara.hutool.core.convert.ConvertUtil;
import org.dromara.hutool.http.HttpUtil;
import org.dromara.hutool.http.client.Request;
import org.dromara.hutool.http.client.Response;
import org.dromara.hutool.http.meta.HeaderName;
import org.dromara.hutool.http.meta.Method;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * MP4视频流分析工具，用于定位moov BOX的位置
 */
public class MP4MoovAnalyzer {

    // MP4 BOX类型常量
    private static final byte[] BOX_TYPE_MOOV = "moov".getBytes();
    private static final byte[] BOX_TYPE_MDAT = "mdat".getBytes();
    private static final byte[] BOX_TYPE_FTYP = "ftyp".getBytes();

    public static void main(String[] args) {
        try {
            // 示例：分析指定URL的MP4视频
            String videoUrl = "https://alist.acecandy.cn:880/d%2Fpt%2FEmby1%2F%E5%9B%BD%E4%BA%A7%E5%89%A7%2F%E5%AD%A4%E5%B2%9B%E9%A3%9E%E9%B9%B0%20%282012%29%2FSeason%201%2F%E5%AD%A4%E5%B2%9B%E9%A3%9E%E9%B9%B0%20-%20S01E21%20-%20%E7%AC%AC21%E9%9B%86%20-%20QHStudIo.mp4";
            MP4MoovAnalyzer analyzer = new MP4MoovAnalyzer();

            // 方法1：直接从HTTP请求分析（推荐）
            MoovPosition position = analyzer.analyzeMoovPositionFromUrl(videoUrl);
            if (position != null) {
                System.out.println("找到moov BOX:");
                System.out.println("  起始位置: " + position.getStartOffset() + " 字节");
                System.out.println("  大小: " + position.getSize() + " 字节");
                System.out.println("  相对位置: " + position.getRelativePosition());
            } else {
                System.out.println("未找到moov BOX，可能需要扩展搜索范围或使用其他方法。");
            }

            // 方法2：从已有字节数组分析（适用于已获取的视频片段）
            /*
            byte[] videoData = ...; // 已有视频数据
            MoovPosition pos = analyzer.analyzeMoovPosition(videoData);
            */

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 从URL分析moov BOX位置，自动处理优化和未优化的MP4文件
     */
    public MoovPosition analyzeMoovPositionFromUrl(String url) throws IOException {
        // 1. 先尝试获取文件总大小
        long fileSize = getFileSize(url);
        System.out.println("文件总大小: " + fileSize + " 字节");

        // 2. 先尝试分析文件头部（适用于优化过的MP4）
        byte[] headData = downloadRange(url, 0, 8192); // 下载前8KB
        MoovPosition position = analyzeMoovPosition(headData);

        if (position != null) {
            position.setRelativePosition("头部");
            return position;
        }

        // 3. 如果头部没有moov，尝试分析尾部（适用于未优化的MP4）
        if (fileSize > 0) {
            long tailStart = Math.max(0, fileSize - 1024 * 1024); // 从最后1MB开始
            byte[] tailData = downloadRange(url, tailStart, fileSize - 1);
            position = analyzeMoovPosition(tailData);

            if (position != null) {
                // 转换为相对于整个文件的位置
                position.setStartOffset(position.getStartOffset() + tailStart);
                position.setRelativePosition("尾部");
                return position;
            }
        }

        // 4. 如果还没找到，尝试结合mdat位置推测moov位置
        MdatPosition mdatPos = findMdatPosition(headData);
        if (mdatPos != null && fileSize > 0) {
            long mdatEnd = mdatPos.getStartOffset() + mdatPos.getSize();
            long moovSearchStart = Math.max(0, mdatEnd);
            long moovSearchEnd = Math.min(fileSize - 1, moovSearchStart + 1024 * 1024);

            byte[] searchData = downloadRange(url, moovSearchStart, moovSearchEnd);
            position = analyzeMoovPosition(searchData);

            if (position != null) {
                position.setStartOffset(position.getStartOffset() + moovSearchStart);
                position.setRelativePosition("mdat之后");
                return position;
            }
        }

        return null; // 未找到moov BOX
    }

    /**
     * 从视频数据中分析moov BOX的位置
     */
    public MoovPosition analyzeMoovPosition(byte[] videoData) {
        long offset = 0;

        while (offset < videoData.length - 8) {
            // 读取BOX大小（前4字节）
            long size = bytesToLong(videoData, (int) offset, 4);

            // 读取BOX类型（接下来4字节）
            byte[] type = Arrays.copyOfRange(videoData, (int) offset + 4, (int) offset + 8);

            // 检查是否为moov BOX
            if (Arrays.equals(type, BOX_TYPE_MOOV)) {
                return new MoovPosition(offset, size);
            }

            // 特殊处理：如果size为0，表示整个文件是一个BOX
            if (size == 0) {
                size = videoData.length - offset;
            }

            // 特殊处理：如果size为1，表示使用扩展大小（接下来8字节）
            if (size == 1 && offset + 16 <= videoData.length) {
                size = bytesToLong(videoData, (int) offset + 8, 8);
            }

            // 移动到下一个BOX
            offset += size;
        }

        return null; // 未找到moov BOX
    }

    /**
     * 查找mdat BOX的位置和大小
     */
    private MdatPosition findMdatPosition(byte[] videoData) {
        long offset = 0;

        while (offset < videoData.length - 8) {
            long size = bytesToLong(videoData, (int) offset, 4);
            byte[] type = Arrays.copyOfRange(videoData, (int) offset + 4, (int) offset + 8);

            if (Arrays.equals(type, BOX_TYPE_MDAT)) {
                return new MdatPosition(offset, size);
            }

            if (size == 0) {
                size = videoData.length - offset;
            }

            if (size == 1 && offset + 16 <= videoData.length) {
                size = bytesToLong(videoData, (int) offset + 8, 8);
            }

            offset += size;
        }

        return null; // 未找到mdat BOX
    }

    /**
     * 下载指定范围的视频数据
     */
    private byte[] downloadRange(String url, long start, long end) throws IOException {
        System.out.println("下载范围: " + start + " - " + end + " 字节");

        Request req = Request.of(url).method(Method.GET)
                .header("Range", "bytes=" + start + "-" + end).setMaxRedirects(1);

        try (Response resp = HttpUtil.send(req); InputStream in = resp.bodyStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            return out.toByteArray();
        }
    }

    /**
     * 获取文件总大小
     */
    private long getFileSize(String url) throws IOException {
        Request req = Request.of(url).method(Method.HEAD).setMaxRedirects(1);
        try (Response resp = HttpUtil.send(req).sync()) {
            return ConvertUtil.toLong(resp.header(HeaderName.CONTENT_LENGTH), -1L);
        }
    }

    /**
     * 将字节数组转换为长整型（支持大端序）
     */
    private long bytesToLong(byte[] bytes, int offset, int length) {
        long result = 0;
        for (int i = 0; i < length; i++) {
            result = (result << 8) | (bytes[offset + i] & 0xFF);
        }
        return result;
    }

    /**
     * 表示moov BOX的位置信息
     */
    public static class MoovPosition {
        private long startOffset;
        private long size;
        private String relativePosition;

        public MoovPosition(long startOffset, long size) {
            this.startOffset = startOffset;
            this.size = size;
        }

        public long getStartOffset() {
            return startOffset;
        }

        public void setStartOffset(long startOffset) {
            this.startOffset = startOffset;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public String getRelativePosition() {
            return relativePosition;
        }

        public void setRelativePosition(String relativePosition) {
            this.relativePosition = relativePosition;
        }
    }

    /**
     * 表示mdat BOX的位置信息
     */
    private static class MdatPosition {
        private long startOffset;
        private long size;

        public MdatPosition(long startOffset, long size) {
            this.startOffset = startOffset;
            this.size = size;
        }

        public long getStartOffset() {
            return startOffset;
        }

        public long getSize() {
            return size;
        }
    }
}