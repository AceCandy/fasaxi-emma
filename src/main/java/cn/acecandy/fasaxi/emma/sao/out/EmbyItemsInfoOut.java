package cn.acecandy.fasaxi.emma.sao.out;

import lombok.Builder;
import lombok.Data;
import org.dromara.hutool.core.annotation.Alias;

import java.util.List;

/**
 * emby item信息输出类
 *
 * @author tangningzhu
 * @since 2025/4/16
 */
@Data
@Builder
public class EmbyItemsInfoOut {

    /**
     * 项目
     */
    @Alias("Items")
    private List<EmbyItem> items;
    /**
     * 总记录数
     */
    @Alias("TotalRecordCount")
    private Integer totalRecordCount;


}