package cn.acecandy.fasaxi.emma.control;

import cn.acecandy.fasaxi.emma.common.resp.Rsres;
import cn.acecandy.fasaxi.emma.sao.out.R123FileListReq;
import cn.acecandy.fasaxi.emma.sao.proxy.R123Proxy;
import cn.acecandy.fasaxi.emma.sao.proxy.R123ZongProxy;
import cn.acecandy.fasaxi.emma.utils.CloudUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.acecandy.fasaxi.emma.common.enums.CloudStorageType.R_123;
import static cn.acecandy.fasaxi.emma.common.enums.CloudStorageType.R_123_ZONG;

/**
 * 用于调试的控制器
 *
 * @author AceCandy
 * @since 2025/08/18
 */
@Slf4j
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @Resource
    private R123Proxy r123Proxy;

    @Resource
    private R123ZongProxy r123ZongProxy;

    @Resource
    private CloudUtil cloudUtil;

    @GetMapping("/123/getAccessTokenByCache")
    public Rsres<Object> getAccessTokenByCache123(boolean force) {
        return Rsres.success(r123Proxy.getAccessTokenByCache(force));
    }

    @GetMapping("/123-zong/getAccessTokenByCache")
    public Rsres<Object> getAccessTokenByCache123zong(boolean force) {
        return Rsres.success(r123ZongProxy.getAccessTokenByCache(force));
    }

    @GetMapping("/123/getFileList")
    public Rsres<Object> getFileList123(R123FileListReq req) {
        return Rsres.success(r123Proxy.getFileList(req));
    }

    @GetMapping("/123-zong/getFileList")
    public Rsres<Object> getFileList123zong(R123FileListReq req) {
        return Rsres.success(r123ZongProxy.getFileList(req));
    }

    @GetMapping("/123/getFile")
    public Rsres<Object> getFile123(String filePath) {
        return Rsres.success(cloudUtil.getFile(R_123, filePath));
    }

    @GetMapping("/123-zong/getFile")
    public Rsres<Object> getFile123zong(String filePath) {
        return Rsres.success(cloudUtil.getFile(R_123_ZONG, filePath));
    }

    @GetMapping("/123/getFileMatch")
    public Rsres<Object> getFileMatch123(String filePath, long size) {
        return Rsres.success(cloudUtil.getFileMatch(R_123, filePath, size));
    }

    @GetMapping("/123-zong/getFileMatch")
    public Rsres<Object> getFileMatch123zong(String filePath, long size) {
        return Rsres.success(cloudUtil.getFileMatch(R_123_ZONG, filePath, size));
    }

    @GetMapping("/123/getDownloadUrl")
    public Rsres<Object> getDownloadUrl123(Long fileId) {
        return Rsres.success(r123Proxy.getDownloadUrl(fileId));
    }

    @GetMapping("/123-zong/getDownloadUrl")
    public Rsres<Object> getDownloadUrl123zong(Long fileId) {
        return Rsres.success(r123ZongProxy.getDownloadUrl(fileId));
    }
}