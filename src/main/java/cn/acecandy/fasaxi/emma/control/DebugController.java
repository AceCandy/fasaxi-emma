package cn.acecandy.fasaxi.emma.control;

import cn.acecandy.fasaxi.emma.common.enums.CloudStorageType;
import cn.acecandy.fasaxi.emma.common.resp.Rsres;
import cn.acecandy.fasaxi.emma.sao.out.R123FileListReq;
import cn.acecandy.fasaxi.emma.sao.proxy.R115Proxy;
import cn.acecandy.fasaxi.emma.sao.proxy.R123Proxy;
import cn.acecandy.fasaxi.emma.sao.proxy.R123ZongProxy;
import cn.acecandy.fasaxi.emma.utils.CloudUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    private R115Proxy r115Proxy;

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

    @GetMapping("/cloud/getFile")
    public Rsres<Object> getFile(String cloud, String filePath) {
        CloudStorageType cloudStorage = CloudStorageType.of(cloud);
        return Rsres.success(cloudUtil.getFile(cloudStorage, filePath));
    }

    @GetMapping("/cloud/getFileMatch")
    public Rsres<Object> getFileMatch(String cloud, String filePath, long size) {
        CloudStorageType cloudStorage = CloudStorageType.of(cloud);
        return Rsres.success(cloudUtil.getFileMatch(cloudStorage, filePath, size));
    }

    @GetMapping("/cloud/getDownloadUrl")
    public Rsres<Object> getDownloadUrl(String cloud, String ua, String filePath, long size) {
        CloudStorageType cloudStorage = CloudStorageType.of(cloud);
        return Rsres.success(cloudUtil.getDownloadUrl(cloudStorage, ua, filePath, size));
    }

    @GetMapping("/cloud/getDownloadUrlOnCopy")
    public Rsres<Object> getDownloadUrlOnCopy(String cloud, String ua, String deviceId,
                                              String filePath, long size) {
        CloudStorageType cloudStorage = CloudStorageType.of(cloud);
        return Rsres.success(cloudUtil.getDownloadUrlOnCopy(cloudStorage, ua, deviceId, filePath, size));
    }

    @GetMapping("/115/addFolder")
    public Rsres<Object> addFolder(String fileName) {
        return Rsres.success(r115Proxy.addFolder(fileName));
    }
}