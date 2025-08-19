package cn.acecandy.fasaxi.emma.sao.client;

import cn.acecandy.fasaxi.emma.sao.config.LogFeignConfig;
import cn.acecandy.fasaxi.emma.sao.out.R115SearchFileReq;
import cn.acecandy.fasaxi.emma.sao.out.R123;
import cn.acecandy.fasaxi.emma.sao.out.R123DownloadUrlResp;
import cn.acecandy.fasaxi.emma.sao.out.R123FileListResp;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

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
    R123<R123FileListResp> searchFile(@RequestHeader("Authorization") String auth,
                                       @SpringQueryMap R115SearchFileReq req);

    /**
     * 获取下载信息
     *
     * @param auth   auth
     * @param fileId 文件id
     * @return {@link R123 }<{@link R123DownloadUrlResp }>
     */
    @GetMapping(path = "/api/v1/file/download_info", headers = {"Platform=open_platform"})
    R123<R123DownloadUrlResp> getDownloadUrl(@RequestHeader("Authorization") String auth,
                                             @RequestParam("fileId") Long fileId);

}