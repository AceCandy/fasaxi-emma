package cn.acecandy.fasaxi.emma.utils;

import lombok.SneakyThrows;
import org.brotli.dec.BrotliInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

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
}