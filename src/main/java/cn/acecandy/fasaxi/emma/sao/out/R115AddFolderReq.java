package cn.acecandy.fasaxi.emma.sao.out;

import lombok.Builder;
import lombok.Data;

/**
 * 新建文件夹
 *
 * @author tangningzhu
 * @since 2025/8/15
 */
@Builder
@Data
public class R115AddFolderReq {
    /**
     * 新建文件夹所在的父目录ID (根目录的ID为0)
     */
    private Long pid;
    /**
     * 新建文件夹名称，限制255个字符
     */
    private String file_name;
}