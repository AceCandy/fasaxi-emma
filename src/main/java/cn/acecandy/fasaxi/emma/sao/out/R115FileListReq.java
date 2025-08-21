package cn.acecandy.fasaxi.emma.sao.out;

import lombok.Builder;
import lombok.Data;

/**
 * 获取115 文件列表
 *
 * @author tangningzhu
 * @since 2025/8/15
 */
@Builder
@Data
public class R115FileListReq {
    /**
     * 目录ID，对应parent_id
     * 是否必须：否
     */
    private Long cid;

    /**
     * 文件类型
     * 1.文档；2.图片；3.音乐；4.视频；5.压缩；6.应用；7.书籍
     * 是否必须：否
     */
    private Integer type;

    /**
     * 查询数量，默认20，最大1150
     * 是否必须：否
     */
    private Integer limit;

    /**
     * 查询起始位，默认0
     * 是否必须：否
     */
    private Integer offset;

    /**
     * 文件后缀名
     * 是否必须：否
     */
    private String suffix;

    /**
     * 排序，1：升序 0：降序
     * 是否必须：否
     */
    private Integer asc;

    /**
     * 排序字段
     * file_name：文件名；file_size：文件大小；user_utime：更新时间；file_type：文件类型
     * 是否必须：否
     */
    private String o;

    /**
     * 是否使用记忆排序
     * 1：使用自定义排序，不使用记忆排序；0：使用记忆排序，自定义排序失效；2：自定义排序，非文件夹置顶
     * 是否必须：否
     */
    private Integer custom_order;

    /**
     * 筛选文件时，是否显示文件夹
     * 1:要展示文件夹；0：不展示
     * 是否必须：否
     */
    private Integer stdir;

    /**
     * 筛选星标文件
     * 1:是；0：全部
     * 是否必须：否
     */
    private Integer star;

    /**
     * 是否只显示当前文件夹内文件
     * 是否必须：否
     */
    private Integer cur;

    /**
     * 是否显示目录
     * 0 或 1，默认为0
     * 是否必须：否
     */
    private Integer show_dir;
}