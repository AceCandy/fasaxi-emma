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
public class R123FileListResp {
    /**
     * -1代表最后一页（无需再翻页查询），其他代表下一页开始的文件id
     */
    private int lastFileId;

    /**
     * 文件列表
     */
    private List<FileInfo> fileList;

    @Data
    public static class FileInfo {
        /**
         * 文件Id
         */
        private long fileId;

        /**
         * 文件名
         */
        private String filename;

        /**
         * 目录ID
         */
        private long parentFileId;

        /**
         * 类型：0-文件 1-文件夹
         */
        private int type;

        /**
         * md5值
         */
        private String etag;

        /**
         * 文件大小
         */
        private long size;

        /**
         * 文件分类：0-未知 1-音频 2-视频 3-图片
         */
        private int category;

        /**
         * 文件审核状态。大于100为审核驳回文件
         */
        private int status;

        /**
         * 惩罚标记（从JSON示例推测）
         */
        private int punishFlag;

        /**
         * S3键标记（从JSON示例推测）
         */
        private String s3KeyFlag;

        /**
         * 存储节点（从JSON示例推测）
         */
        private String storageNode;

        /**
         * 文件是否在回收站标识：0 否 1是
         */
        private int trashed;

        /**
         * 创建时间
         */
        private String createAt;

        /**
         * 更新时间
         */
        private String updateAt;
    }
}