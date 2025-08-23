package cn.acecandy.fasaxi.emma.sao.out;

import lombok.Data;

import java.util.List;

/**
 * 获取123 文件列表
 *
 * @author tangningzhu
 * @since 2025/8/15
 */
@Data
public class R115FileInfoResp {
    /**
     * 包含文件总数量
     */
    private String count;

    /**
     * 文件(夹)总大小
     */
    private String size;

    /**
     * 文件(夹)总大小(字节单位)
     */
    private Long size_byte;

    /**
     * 包含文件夹总数量
     */
    private String folder_count;

    /**
     * 视频时长；-1：正在统计，其他数值为视频时长的数值(单位秒)
     */
    private Integer play_long;

    /**
     * 是否开启展示视频时长
     */
    private Integer show_play_long;

    /**
     * 上传时间
     */
    private String ptime;

    /**
     * 修改时间
     */
    private String utime;

    /**
     * 文件名
     */
    private String file_name;

    /**
     * 文件提取码
     */
    private String pick_code;

    /**
     * sh1值
     */
    private String sha1;

    /**
     * 文件(夹)ID
     */
    private Long file_id;

    /**
     * 是否星标
     */
    private String is_mark;

    /**
     * 文件(夹)最近打开时间
     */
    private Long open_time;

    /**
     * 文件属性；1；文件；0：文件夹
     */
    private Integer file_category;

    /**
     * 文件(夹)所在的路径
     */
    private List<Paths> paths;

    /**
     * 路径信息内部类
     */
    @Data
    public static class Paths {

        /**
         * 父目录ID
         */
        private Long file_id;

        /**
         * 父目录名称
         */
        private String file_name;
    }
}