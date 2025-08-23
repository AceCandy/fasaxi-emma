package cn.acecandy.fasaxi.emma.sao.out;

import lombok.Builder;
import lombok.Data;

/**
 * 获取115 文件信息
 *
 * @author tangningzhu
 * @since 2025/8/15
 */
@Builder
@Data
public class R115DownloadUrlReq {
    /**
     * 文件提取码
     */
    private String pick_code;

}