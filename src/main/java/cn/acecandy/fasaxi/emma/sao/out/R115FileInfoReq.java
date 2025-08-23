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
public class R115FileInfoReq {
    /**
     * 文件路径；分隔符支持 / > 两种符号，
     * 最前面需分隔符开头，以分隔符分隔目录层级；
     * 和file_id必传一个
     */
    private String path;

}