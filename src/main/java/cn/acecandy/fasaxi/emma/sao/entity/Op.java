package cn.acecandy.fasaxi.emma.sao.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

import static cn.acecandy.fasaxi.emma.common.enums.ErrCode.DEFAULT_CODE2;

/**
 * openlist 实体类
 *
 * @author tangningzhu
 * @since 2025/10/17
 */
public record Op<T>(Integer code, String message, T data) {
    @JsonIgnore
    public boolean isOk() {
        return DEFAULT_CODE2.getCode().equals(code);
    }

    /**
     * 登录 请求入参
     *
     * @author AceCandy
     * @since 2025/10/17
     */
    public record LoginReq(String username, String password) {
    }

    /**
     * 登录 请求出参
     *
     * @author AceCandy
     * @since 2025/10/17
     */
    public record LoginResp(String token) {
    }

    /**
     * 创建目录 请求入参
     *
     * @param path 需要创意的目录路径(全路径)
     * @author AceCandy
     * @since 2025/10/17
     */
    public record MkdirReq(String path) {
    }

    /**
     * 重命名 请求入参
     *
     * @param path 源文件(全路径包含文件名)
     * @param name 目标文件名
     * @author AceCandy
     * @since 2025/10/17
     */
    public record RenameReq(String path, String name) {
    }

    /**
     * 列表 请求入参
     *
     * @param path     目录路径(全路径) 非必填
     * @param password 目录密码 非必填
     * @param page     页数 非必填
     * @param per_page 每页数目 非必填
     * @param refresh  是否强制刷新 非必填
     * @author AceCandy
     * @since 2025/10/17
     */
    public record ListReq(String path, String password, Integer page,
                          Integer per_page, Boolean refresh) {
    }

    /**
     * 列表 请求出参
     *
     * @param content  内容
     * @param total    总数
     * @param readme   说明
     * @param write    是否可写入
     * @param provider 提供方
     * @param header   头信息
     * @author AceCandy
     * @since 2025/10/17
     */
    public record ListResp(List<ListContent> content, Integer total, String readme, Boolean write,
                           String provider, String header) {
    }

    /**
     * 列表 请求出参 内容
     *
     * @param id       文件id
     * @param name     文件名
     * @param size     大小
     * @param is_dir   是否是文件夹
     * @param modified 修改时间
     * @param sign     签名
     * @param thumb    缩略图
     * @param type     类型
     * @param created  创建时间
     * @param hashinfo 哈希信息
     * @author AceCandy
     * @since 2025/10/17
     */
    public record ListContent(String id, String name, Long size, Boolean is_dir,
                              String created, String modified, String sign, String thumb, Integer type,
                              String hashinfo) {
    }

    /**
     * 复制文件 请求入参
     *
     * @param src_dir   源文件夹
     * @param dst_dir   目标文件夹
     * @param names     文件名 多个
     * @param overwrite 是否覆盖
     * @author AceCandy
     * @since 2025/10/17
     */
    public record CopyReq(String src_dir, String dst_dir, List<String> names, Boolean overwrite) {
    }

    /**
     * 删除文件或文件夹 请求入参
     *
     * @param dir   目录
     * @param names 文件名 多个
     * @author AceCandy
     * @since 2025/10/17
     */
    public record RemoveReq(String dir, List<String> names) {
    }
}