package cn.acecandy.fasaxi.emma.sao.out;

import lombok.Data;

/**
 * 获取123 文件列表
 *
 * @author tangningzhu
 * @since 2025/8/15
 */
@Data
public class R115DownloadUrlResp {
    /**
     * 文件名
     */
    private String file_name;

    /**
     * 文件大小（单位通常为字节）
     */
    private long file_size;

    /**
     * 文件提取码
     * 用于获取文件的权限验证，可能为null或空字符串
     */
    private String pick_code;

    /**
     * 文件的sha1值
     * 用于校验文件完整性和唯一性
     */
    private String sha1;

    /**
     * 下载地址信息对象
     */
    private UrlInfo url;

    /**
     * 下载地址信息内部类
     */
    @Data
    public static class UrlInfo {
        /**
         * 文件下载地址
         * 可直接用于下载文件的URL
         */
        private String url;
    }
}