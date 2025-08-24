package cn.acecandy.fasaxi.emma.sao.dto;

import cn.acecandy.fasaxi.emma.common.enums.CloudStorageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static cn.acecandy.fasaxi.emma.common.enums.CloudStorageType.R_115;
import static cn.acecandy.fasaxi.emma.common.enums.CloudStorageType.R_123;
import static cn.acecandy.fasaxi.emma.common.enums.CloudStorageType.R_123_ZONG;

/**
 * 云盘文件DTO
 *
 * @author tangningzhu
 * @since 2025/8/19
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Rile {
    /**
     * 文件id
     */
    public Long fileId;
    /**
     * 文件名
     */
    public String fileName;
    /**
     * 文件大小
     */
    public Long fileSize;
    /**
     * 115 pikcode
     */
    public String pickCode;
    /**
     * 文件类型
     * <p>
     * 0-文件夹 1-文件
     * 115 和123该值不同 需要手动匹配
     */
    public Integer fileType;

    public void setFileType(CloudStorageType cloudStorage, Integer cloudFileType) {
        if (R_115.equals(cloudStorage)) {
            this.fileType = cloudFileType;
        } else if (R_123.equals(cloudStorage) || R_123_ZONG.equals(cloudStorage)) {
            // 123 0-文件 1-文件夹
            if (cloudFileType == 0) {
                this.fileType = 1;
            } else if (cloudFileType == 1) {
                this.fileType = 0;
            }
        }
    }
}