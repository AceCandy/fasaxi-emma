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
public class R115FileListResp {
    /**
     * 文件ID
     */
    private Long fid;

    /**
     * 文件的状态，aid 的别名。1 正常，7 删除(回收站)，120 彻底删除
     */
    private String aid;

    /**
     * 父目录ID
     */
    private Long pid;

    /**
     * 文件分类。0 文件夹，1 文件
     */
    private Integer fc;

    /**
     * 文件(夹)名称
     */
    private String fn;

    /**
     * 文件夹封面
     */
    private String fco;

    /**
     * 是否星标，1：星标
     */
    private String ism;

    /**
     * 是否加密；1：加密
     */
    private Integer isp;

    /**
     * 文件提取码 pickcode
     */
    private String pc;

    /**
     * 修改时间
     */
    private Integer upt;

    /**
     * 修改时间
     */
    private Integer uet;

    /**
     * 上传时间
     */
    private Integer uppt;

    /**
     * 未知字段
     */
    private Integer cm;

    /**
     * 文件备注
     */
    private String fdesc;

    /**
     * 是否统计文件夹下视频时长开关
     */
    private Integer ispl;

    /**
     * 文件标签列表
     */
    private List<FileLabel> fl;

    /**
     * sha1值
     */
    private String sha1;

    /**
     * 文件大小
     */
    private Long fs;

    /**
     * 文件状态 0/2 未上传完成，1 已上传完成
     */
    private String fta;

    /**
     * 文件后缀名
     */
    private String ico;

    /**
     * 音频长度
     */
    private String fatr;

    /**
     * 是否为视频
     */
    private Integer isv;

    /**
     * 视频清晰度；1:标清 2:高清 3:超清 4:1080P 5:4k;100:原画
     */
    private Integer def;

    /**
     * 视频清晰度；1:标清 2:高清 3:超清 4:1080P 5:4k;100:原画
     */
    private Integer def2;

    /**
     * 音视频时长
     */
    private Long playLong;

    /**
     * 未知字段
     */
    private String vImg;

    /**
     * 图片缩略图
     */
    private String thumb;

    /**
     * 原图地址
     */
    private String uo;

    @Data
    public static class FileLabel {

        /**
         * 文件标签id
         */
        private String id;

        /**
         * 文件标签名称
         */
        private String name;

        /**
         * 文件标签排序
         */
        private String sort;

        /**
         * 文件标签颜色
         */
        private String color;

        /**
         * 文件标签类型；0：最近使用；1：非最近使用；2：为默认标签
         */
        private Integer isDefault;

        /**
         * 文件标签更新时间
         */
        private Integer updateTime;

        /**
         * 文件标签创建时间
         */
        private Integer createTime;
    }
}