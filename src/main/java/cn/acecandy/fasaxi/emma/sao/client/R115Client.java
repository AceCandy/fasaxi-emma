package cn.acecandy.fasaxi.emma.sao.client;

import cn.acecandy.fasaxi.emma.sao.config.LogFeignConfig;
import cn.acecandy.fasaxi.emma.sao.out.R115File;
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
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

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
}