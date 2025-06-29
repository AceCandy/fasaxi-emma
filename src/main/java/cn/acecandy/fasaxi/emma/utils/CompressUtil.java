package cn.acecandy.fasaxi.emma.utils;

import lombok.SneakyThrows;
import org.brotli.dec.BrotliInputStream;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 压缩util
 *
 * @author AceCandy
 * @since 2025/05/05
 */
public final class CompressUtil extends org.dromara.hutool.extra.compress.CompressUtil {

    private CompressUtil() {
    }

    /**
     * 解读
     *
     * @param input 输入
     * @return {@link byte[] }
     * @throws IOException IOException
     */
    @SneakyThrows
    public static byte[] decode(byte[] input) {
        if (input == null || input.length == 0) {
            return new byte[0]; // 处理空输入
        }
        try (ByteArrayInputStream bis = new ByteArrayInputStream(input);
             BrotliInputStream brotliInputStream = new BrotliInputStream(bis);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = brotliInputStream.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            return bos.toByteArray();
        }
    }

    /**
     * 是否gzip压缩
     *
     * @param inputStream 输入流
     * @return boolean
     * @throws IOException IOException
     */
    public static boolean isGzipCompressed(InputStream inputStream) throws IOException {
        // 保存当前位置以便重置流
        if (!inputStream.markSupported()) {
            inputStream = new BufferedInputStream(inputStream);
        }
        inputStream.mark(2);

        // 检查前两个字节是否是GZIP的魔数(0x1F 0x8B)
        int b1 = inputStream.read();
        int b2 = inputStream.read();
        inputStream.reset();

        return (b1 == 0x1F && b2 == 0x8B);
    }
}