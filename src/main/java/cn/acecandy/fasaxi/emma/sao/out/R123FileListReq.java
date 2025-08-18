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
public class R123FileListReq {
    /**
     * 文件夹ID，根目录传 0
     * <p>
     * 必填
     */
    private Long parentFileId;
    /**
     * 每页文件数量，最大不超过100
     * <p>
     * 必填
     */
    private Integer limit;
    /**
     * 搜索关键字将无视文件夹ID参数。将会进行全局查找
     */
    private String searchData;
    /**
     * 0:全文模糊搜索(注:将会根据搜索项分词,查找出相似的匹配项)
     * 1:精准搜索(注:精准搜索需要提供完整的文件名)
     */
    private Integer searchMode;
    /**
     * 最后一个文件id
     * <p>
     * 翻页查询时需要填写
     */
    private Long lastFileId;
}