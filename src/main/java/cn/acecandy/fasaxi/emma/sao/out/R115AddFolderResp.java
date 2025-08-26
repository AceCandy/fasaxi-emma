package cn.acecandy.fasaxi.emma.sao.out;

import lombok.Data;

/**
 * 新建文件夹
 *
 * @author tangningzhu
 * @since 2025/8/15
 */
@Data
public class R115AddFolderResp {
    /**
     * 新建的文件夹ID
     */
    private Long file_id;
    /**
     * 新建的文件夹名称
     */
    private String file_name;
}