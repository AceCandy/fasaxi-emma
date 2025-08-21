package cn.acecandy.fasaxi.emma.sao.out;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * 123返回类封装
 *
 * @author tangningzhu
 * @since 2025/8/15
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
public class R115File<T> extends R115<T> {
    /**
     * 当前目录文件数量
     */
    private Integer count;
    /**
     * 系统文件夹数量
     */
    private Integer sysCount;
    /**
     * 分页获取值
     */
    private Integer limit;
    /**
     * 偏移
     */
    private Integer offset;

    /**
     * 文件的状态，aid 的别名。1 正常，7 删除(回收站)，120 彻底删除
     */
    private String aid;

    /**
     * 父目录id
     */
    private Long cid;

    /**
     * 排序，1：升序 0：降序
     */
    private Integer isAsc;

    /**
     * 最小文件大小
     */
    private Long minSize;

    /**
     * 最大文件大小
     */
    private Long maxSize;

    /**
     * 系统目录
     */
    private String sysDir;

    /**
     * 是否返回文件数据
     */
    private String hideData;

    /**
     * 是否记录文件夹的打开时间
     */
    private String recordOpenTime;

    /**
     * 是否星标；1：星标；0：未星标
     */
    private Integer star;

    /**
     * 一级筛选大分类，1：文档，2：图片，3：音乐，4：视频，5：压缩包，6：应用
     */
    private Integer type;

    /**
     * 一级筛选选其他时填写的后缀名
     */
    private String suffix;

    /**
     * 父目录树
     */
    private List<PathItem> path;

    /**
     * 当前标识
     */
    private Integer cur;

    /**
     * 文件夹显示标识
     */
    private Integer stdir;

    /**
     * 字段信息
     */
    private String fields;

    /**
     * 排序方式
     */
    private String order;

    /**
     * 父目录树实体类
     */
    @Data
    public static class PathItem {

        /**
         * 父目录文件名称
         */
        private String name;

        /**
         * 状态标识
         */
        private Object aid;

        /**
         * 目录id
         */
        private Long cid;

        /**
         * 父目录id
         */
        private Object pid;

        /**
         * 加密标识
         */
        private Object isp;

        /**
         * 父目录cid
         */
        private String pCid;

        /**
         * 未知字段
         */
        private String fv;
    }
}