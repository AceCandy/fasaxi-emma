package cn.acecandy.fasaxi.emma.sao.client;

import cn.acecandy.fasaxi.emma.sao.config.LogFeignConfig;
import cn.acecandy.fasaxi.emma.sao.out.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 115 client
 *
 * @author tangningzhu
 * @since 2025/8/15
 */
@FeignClient(name = "R115Client", url = "${cloud.r115.host}",
        configuration = LogFeignConfig.class)
public interface R115Client {

    /**
     * 获取文件列表
     *
     * @param auth auth
     * @param req  请求入参
     * @return {@link R123 }<{@link R123FileListResp }>
     */
    @GetMapping(path = "/open/ufile/search")
    R115Search<List<R115SearchFileResp>> searchFile(@RequestHeader("Authorization") String auth,
                                                    @SpringQueryMap R115SearchFileReq req);

    /**
     * 获取文件列表
     *
     * @param auth auth
     * @param req  请求入参
     * @return {@link R123 }<{@link R123FileListResp }>
     */
    @GetMapping(path = "/open/ufile/files")
    R115File<List<R115FileListResp>> getFileList(@RequestHeader("Authorization") String auth,
                                                 @SpringQueryMap R115FileListReq req);

    /**
     * 获取文件信息
     *
     * @param auth   auth
     * @param fileId 文件id
     * @param req    请求入参
     * @return {@link R115File }<{@link R115FileInfoResp }>
     */
    @GetMapping(path = "/open/folder/get_info")
    R115<Object> getFileInfo(@RequestHeader("Authorization") String auth,
                             @RequestParam("file_id") Long fileId,
                             @SpringQueryMap R115FileInfoReq req);

    /**
     * 获取下载链接
     *
     * @param auth      auth
     * @param userAgent 用户代理
     * @param req       雷克
     * @return {@link R115File }<{@link R115FileInfoResp }>
     */
    @PostMapping(path = "/open/ufile/downurl", consumes = "application/x-www-form-urlencoded")
    R115<Map<Long, R115DownloadUrlResp>> getDownloadUrl(@RequestHeader("Authorization") String auth,
                                                        @RequestHeader("User-Agent") String userAgent,
                                                        R115DownloadUrlReq req);

    /**
     * 批量复制文件
     *
     * @param auth auth
     * @param req  雷克
     * @return {@link R115File }<{@link R115FileInfoResp }>
     */
    @PostMapping(path = "/open/ufile/copy", consumes = "application/x-www-form-urlencoded")
    R115<Object> copyFile(@RequestHeader("Authorization") String auth,
                          R115CopyReq req);

    /**
     * 新建文件夹
     *
     * @param auth auth
     * @param req  雷克
     * @return {@link R115File }<{@link R115FileInfoResp }> 出参报错的时候是个list
     */
    @PostMapping(path = "/open/folder/add", consumes = "application/x-www-form-urlencoded")
    R115<Object> addFolder(@RequestHeader("Authorization") String auth,
                           R115AddFolderReq req);
}