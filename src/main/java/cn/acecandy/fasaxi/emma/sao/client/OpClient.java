package cn.acecandy.fasaxi.emma.sao.client;

import cn.acecandy.fasaxi.emma.sao.config.LogFeignConfig;
import cn.acecandy.fasaxi.emma.sao.entity.Op;
import cn.acecandy.fasaxi.emma.sao.out.R123;
import cn.acecandy.fasaxi.emma.sao.out.R123TokenResp;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * openlist 鉴权客户端
 *
 * @author tangningzhu
 * @since 2025/8/15
 */
@FeignClient(name = "OpAuthClient", url = "${openlist.host}",
        configuration = LogFeignConfig.class)
public interface OpClient {

    /**
     * 获取访问token 有效期48小时
     *
     * @param req 请求入参
     * @return {@link R123 }<{@link R123TokenResp }>
     */
    @PostMapping(path = "/api/auth/login")
    Op<Op.LoginResp> login(@RequestBody Op.LoginReq req);

    /**
     * 创建目录
     *
     * @param auth 认证
     * @param req  请求入参
     * @return {@link Op }
     */
    @PostMapping(path = "/api/fs/mkdir")
    Op<Void> mkdir(@RequestHeader("Authorization") String auth,
                   @RequestBody Op.MkdirReq req);

    /**
     * 重命名
     *
     * @param auth 认证
     * @param req  请求入参
     * @return {@link Op }<{@link Void }>
     */
    @PostMapping(path = "/api/fs/rename")
    Op<Void> rename(@RequestHeader("Authorization") String auth,
                    @RequestBody Op.RenameReq req);

    /**
     * 列出文件目录
     *
     * @param auth 认证
     * @param req  请求入参
     * @return {@link Op }<{@link Op.ListResp }>
     */
    @PostMapping(path = "/api/fs/list")
    Op<Op.ListResp> list(@RequestHeader("Authorization") String auth,
                         @RequestBody Op.ListReq req);

    /**
     * 复制文件
     *
     * @param auth 认证
     * @param req  请求入参
     * @return {@link Op }<{@link Void }>
     */
    @PostMapping(path = "/api/fs/copy")
    Op<Void> copy(@RequestHeader("Authorization") String auth,
                  @RequestBody Op.CopyReq req);

    /**
     * 删除文件或文件夹
     *
     * @param auth 认证
     * @param req  请求入参
     * @return {@link Op }<{@link Void }>
     */
    @PostMapping(path = "/api/fs/remove")
    Op<Void> remove(@RequestHeader("Authorization") String auth,
                    @RequestBody Op.RemoveReq req);
}