package cn.acecandy.fasaxi.emma.utils;

import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.io.file.FileMode;
import org.dromara.hutool.core.io.file.FileUtil;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * 读取文件指定范围的数据
 *
 * @author tangningzhu
 * @since 2025/4/25
 */
@Slf4j
public class FileRangeReader implements Iterable<byte[]> {
    /**
     * 文件路径
     */
    private final String filePath;
    /**
     * 起点
     */
    private final long startPoint;
    /**
     * 终点
     */
    private final Long endPoint;
    /**
     * 每次读取的字节数
     */
    private final int chunkSize;

    public FileRangeReader(String filePath, long startPoint, Long endPoint, int chunkSize) {
        this.filePath = filePath;
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.chunkSize = chunkSize;
    }

    @Override
    public Iterator<byte[]> iterator() {
        return new Iterator<>() {
            private RandomAccessFile file;
            private long currentPosition = startPoint;
            private long fileLength;

            // 初始化文件读取器
            private void initReader() {
                if (null != file) {
                    return;
                }
                try {
                    file = FileUtil.createRandomAccessFile(Paths.get(filePath), FileMode.r);
                    fileLength = file.length();
                    file.seek(startPoint);
                } catch (IOException e) {
                    log.warn("初始化文件失败: {}", filePath, e);
                }
            }

            // 判断是否还有下一个元素
            @Override
            public boolean hasNext() {
                initReader();
                if (file == null) {
                    return false;
                }
                if (endPoint != null) {
                    return currentPosition < endPoint + 1;
                }
                return currentPosition < fileLength;
            }

            // 获取下一个元素
            @Override
            public byte[] next() {
                initReader();
                if (file == null) {
                    return new byte[0];
                }
                int readSize = chunkSize;
                if (endPoint != null) {
                    long remaining = (endPoint + 1) - currentPosition;
                    if (remaining <= 0) {
                        return new byte[0];
                    }
                    readSize = (int) Math.min(readSize, remaining);
                }
                if (currentPosition + readSize > fileLength) {
                    readSize = (int) (fileLength - currentPosition);
                }
                try {
                    byte[] buffer = new byte[readSize];
                    int bytesRead = file.read(buffer);
                    if (bytesRead == -1) {
                        return new byte[0];
                    }
                    currentPosition += bytesRead;
                    if (bytesRead < readSize) {
                        byte[] actualBuffer = new byte[bytesRead];
                        System.arraycopy(buffer, 0, actualBuffer, 0, bytesRead);
                        return actualBuffer;
                    }
                    return buffer;
                } catch (IOException e) {
                    log.warn("获取文件下一chunk失败: {}", filePath, e);
                    file = null;
                    return new byte[0];
                }
            }
        };
    }

    // 返回一个流，用于处理文件读取的数据
    public Stream<byte[]> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    public static void main(String[] args) {
        String filePath = "/Users/mac/Downloads/2024-10-12 10.21.14.mp4";
        // long startPoint = 0;
        // Long endPoint = 1000L; // 结束点
        // int chunkSize = 100; // 每次读取的字节数
        long startPoint = 10;
        Long endPoint = null;
        int chunkSize = 1024;
        FileRangeReader reader = new FileRangeReader(filePath, startPoint, endPoint, chunkSize);
        reader.stream().forEach(chunk -> {
            // 处理每个数据块
            // int i = 16 + 1;
            System.out.println("Read chunk of size: " + chunk.length);
        });
    }
}