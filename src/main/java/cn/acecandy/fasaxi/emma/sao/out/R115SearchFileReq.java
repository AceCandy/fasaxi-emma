package cn.acecandy.fasaxi.emma.sao.out;

import lombok.Builder;
import lombok.Data;

/**
 * 获取123 文件列表
 *
 * @author tangningzhu
 * @since 2025/8/15
 */
@Builder
@Data
public class R115SearchFileReq {
    /**
     * 查找关键字
     * 是必填参数
     */
    private String search_value;

    /**
     * 单页记录数
     * 是必填参数，默认20，offset+limit最大不超过10000
     */
    private Integer limit;

    /**
     * 数据显示偏移量
     * 是必填参数
     */
    private Integer offset;

    /**
     * 文件标签搜索
     * 非必填参数，支持按文件标签筛选
     */
    private String file_label;

    /**
     * 目标目录cid
     * 非必填参数，cid=-1时，表示不返回列表任何内容
     */
    private Integer cid;

    /**
     * 搜索结果匹配的开始时间
     * 非必填参数，格式：2020-11-19
     */
    private String gte_day;

    /**
     * 搜索结果匹配的结束时间
     * 非必填参数，格式：2020-11-20
     */
    private String lte_day;

    /**
     * 显示类型筛选（文件或文件夹）
     * 非必填参数，1 只显示文件夹，2 只显示文件
     */
    private Integer fc;

    /**
     * 一级筛选大分类
     * 非必填参数，1：文档，2：图片，3：音乐，4：视频，5：压缩包，6：应用
     */
    private Integer type;

    /**
     * 一级筛选选其他时填写的后缀名
     * 非必填参数，当type选择其他分类时使用
     */
    private String suffix;
}