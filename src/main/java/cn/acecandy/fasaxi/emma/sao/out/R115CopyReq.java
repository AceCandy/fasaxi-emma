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
public class R115CopyReq {
    /**
     * 目标目录，即所需移动到的目录
     */
    private Long pid;
    /**
     * 所复制的文件和目录ID，多个文件和目录请以 , 隔开
     */
    private Long file_id;

    /**
     * 复制的文件在目标目录是否允许重复，默认0：0：可以；1：不可以
     * <p>
     * 非必填
     */
    private Integer nodupli;

}