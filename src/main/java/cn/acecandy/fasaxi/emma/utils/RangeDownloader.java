package cn.acecandy.fasaxi.emma.utils;


import org.dromara.hutool.core.lang.Console;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class RangeDownloader {

    /**
     * 通过Range请求下载指定字节范围的数据并保存为文件
     *
     * @param urlStr    目标文件URL
     * @param savePath  本地保存路径（如：/path/to/save.mp4）
     * @param startByte 下载起始字节（从0开始）
     * @param endByte   下载结束字节（包含，如10MB为10485759）
     * @throws Exception 网络或IO异常
     */
    public static void downloadWithRange(String urlStr, String savePath, long startByte, long endByte) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // 设置请求头：Range获取指定字节范围
        conn.setRequestMethod("GET");
        // conn.setRequestProperty("Range", "bytes=" + startByte + "-" + endByte);
        conn.setRequestProperty("Range", "bytes=" + startByte + "-");
        conn.setConnectTimeout(5000); // 连接超时5秒
        conn.setReadTimeout(100000);   // 读取超时10秒

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_PARTIAL || responseCode == HttpURLConnection.HTTP_OK
            //|| responseCode == HttpURLConnection.HTTP_MOVED_TEMP
        ) {
            // 获取响应流
            Console.log("返回码:{}", responseCode);
            try (InputStream inputStream = conn.getInputStream();
                 FileOutputStream fileOutputStream = new FileOutputStream(savePath);
                 BufferedInputStream bis = new BufferedInputStream(inputStream);
                 BufferedOutputStream bos = new BufferedOutputStream(fileOutputStream)) {

                byte[] buffer = new byte[1024 * 1024]; // 1MB缓冲区
                int bytesRead;
                while ((bytesRead = bis.read(buffer)) != -1) {
                    bos.write(buffer, 0, bytesRead);
                    Console.log("保存{}", bytesRead);
                }
                System.out.println("下载完成，保存至：" + savePath);
            }
        } else {
            throw new IOException("Range请求失败，响应码：" + responseCode);
        }
        conn.disconnect();
    }
}