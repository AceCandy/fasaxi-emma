package cn.acecandy.fasaxi.emma.sao.out;

import lombok.Data;

/**
 * tmdb 图片信息输出类
 *
 * @author tangningzhu
 * @since 2025/4/16
 */

/**
 * dataEye 内容
 *
 * @author AceCandy
 * @since 2025/12/07
 */
@Data
public class DataEyeRank {
    /**
     * 排名
     */
    private Integer ranking;

    /**
     * 短剧ID
     */
    private Long playletId;

    /**
     * 短剧名称
     */
    private String playletName;

    /**
     * 关联方（可为null）
     */
    /*private String relatedParty;

     *//**
     * 总消费数
     *//*
    private Long totalConsumeNum;

    *//**
     * 消费数
     *//*
    private Long consumeNum;

    *//**
     * 新标识
     *//*
    private Boolean newFlag;

    *//**
     * 顶部数量/排名位置
     *//*
    private Integer topNum;

    *//**
     * 封面OSS地址
     *//*
    private String coverOss;

    *//**
     * 关联方公司列表
     *//*
    private List<String> relatedPartyCompany;

    *//**
     * 承包方公司列表
     *//*
    private List<String> contractorCompany;

    *//**
     * 版权方公司列表
     *//*
    private List<String> copyrightHolderCompany;

    *//**
     * 短剧标签列表（包含null元素）
     *//*
    private List<String> playletTags;*/

    /**
     * 统一短剧ID
     */
    private Long unifiedPlayletId;

    /**
     * 海外短剧名称
     */
    private String overseasPlayletName;
}