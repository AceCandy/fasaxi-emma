package cn.acecandy.fasaxi.emma.sao.client;

import cn.acecandy.fasaxi.emma.sao.config.LogFeignConfig;
import cn.acecandy.fasaxi.emma.sao.out.R115;
import cn.acecandy.fasaxi.emma.sao.out.R115DownloadUrlReq;
import cn.acecandy.fasaxi.emma.sao.out.R115DownloadUrlResp;
import cn.acecandy.fasaxi.emma.sao.out.R115File;
import cn.acecandy.fasaxi.emma.sao.out.R115FileInfoReq;
import cn.acecandy.fasaxi.emma.sao.out.R115FileInfoResp;
import cn.acecandy.fasaxi.emma.sao.out.R115FileListReq;
import cn.acecandy.fasaxi.emma.sao.out.R115FileListResp;
import cn.acecandy.fasaxi.emma.sao.out.R115Search;
import cn.acecandy.fasaxi.emma.sao.out.R115SearchFileReq;
import cn.acecandy.fasaxi.emma.sao.out.R115SearchFileResp;
import cn.acecandy.fasaxi.emma.sao.out.R123;
import cn.acecandy.fasaxi.emma.sao.out.R123FileListResp;
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
    R115<R115FileInfoResp> getFileInfo(@RequestHeader("Authorization") String auth,
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
}