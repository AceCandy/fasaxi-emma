package cn.acecandy.fasaxi.emma.sao.out;

import lombok.Data;

/**
 * 获取115 文件列表
 *
 * @author tangningzhu
 * @since 2025/8/15
 */
@Data
public class R115SearchFileResp {
    /**
     * 文件ID
     */
    private Long file_id;

    /**
     * 用户ID
     */
    private String user_id;

    /**
     * 文件sha1值
     */
    private String sha1;

    /**
     * 文件名称
     */
    private String file_name;

    /**
     * 文件大小
     */
    private Long file_size;

    /**
     * 上传时间
     */
    private String user_ptime;

    /**
     * 更新时间
     */
    private String user_utime;

    /**
     * 文件提取码
     */
    private String pick_code;

    /**
     * 父目录ID
     */
    private String parent_id;

    /**
     * 文件的状态，aid 的别名
     * 1：正常，7：删除(回收站)，120：彻底删除
     */
    private String area_id;

    /**
     * 文件是否隐藏
     * 0：未隐藏，1：已隐藏
     */
    private Integer is_private;

    /**
     * 文件类别
     * 0：文件夹 1：文件；
     */
    private Integer file_category;

    /**
     * 文件后缀
     */
    private String ico;
}