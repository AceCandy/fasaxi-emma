package cn.acecandy.fasaxi.emma.control;

import cn.acecandy.fasaxi.emma.common.enums.CloudStorageType;
import cn.acecandy.fasaxi.emma.common.resp.Rsres;
import cn.acecandy.fasaxi.emma.sao.out.R123FileListReq;
import cn.acecandy.fasaxi.emma.sao.proxy.R115Proxy;
import cn.acecandy.fasaxi.emma.sao.proxy.R123Proxy;
import cn.acecandy.fasaxi.emma.sao.proxy.R123ZongProxy;
import cn.acecandy.fasaxi.emma.utils.CloudUtil;
import cn.hutool.v7.core.text.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public Rsres<Object> getAccessTokenByCache123(@RequestParam(defaultValue = "false") boolean force) {
        return Rsres.success(r123Proxy.getAccessTokenByCache(force));
    }

    @GetMapping("/123-zong/getAccessTokenByCache")
    public Rsres<Object> getAccessTokenByCache123zong(@RequestParam(defaultValue = "false") boolean force) {
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
    public ResponseEntity<Rsres<Object>> getFile(@RequestParam String cloud, @RequestParam String filePath) {
        CloudStorageType cloudStorage = requireCloudStorage(cloud);
        if (cloudStorage == null) {
            return badRequest("不支持的 cloud 参数: " + cloud);
        }
        if (StrUtil.isBlank(filePath)) {
            return badRequest("filePath 不能为空");
        }
        return ResponseEntity.ok(Rsres.success(cloudUtil.getFile(cloudStorage, filePath)));
    }

    @GetMapping("/cloud/getFileMatch")
    public ResponseEntity<Rsres<Object>> getFileMatch(@RequestParam String cloud,
                                                      @RequestParam String filePath,
                                                      @RequestParam long size) {
        CloudStorageType cloudStorage = requireCloudStorage(cloud);
        if (cloudStorage == null) {
            return badRequest("不支持的 cloud 参数: " + cloud);
        }
        if (StrUtil.isBlank(filePath)) {
            return badRequest("filePath 不能为空");
        }
        if (size < 0) {
            return badRequest("size 不能小于 0");
        }
        return ResponseEntity.ok(Rsres.success(cloudUtil.getFileMatch(cloudStorage, filePath, size)));
    }

    @GetMapping("/cloud/getDownloadUrl")
    public ResponseEntity<Rsres<Object>> getDownloadUrl(@RequestParam String cloud,
                                                        @RequestParam String ua,
                                                        @RequestParam String filePath,
                                                        @RequestParam long size) {
        CloudStorageType cloudStorage = requireCloudStorage(cloud);
        if (cloudStorage == null) {
            return badRequest("不支持的 cloud 参数: " + cloud);
        }
        if (StrUtil.hasBlank(ua, filePath)) {
            return badRequest("ua 和 filePath 不能为空");
        }
        if (size < 0) {
            return badRequest("size 不能小于 0");
        }
        return ResponseEntity.ok(Rsres.success(cloudUtil.getDownloadUrl(cloudStorage, ua, filePath, size)));
    }

    @GetMapping("/cloud/getDownloadUrlOnCopy")
    public ResponseEntity<Rsres<Object>> getDownloadUrlOnCopy(@RequestParam String cloud,
                                                              @RequestParam String ua,
                                                              @RequestParam String deviceId,
                                                              @RequestParam String filePath,
                                                              @RequestParam long size) {
        CloudStorageType cloudStorage = requireCloudStorage(cloud);
        if (cloudStorage == null) {
            return badRequest("不支持的 cloud 参数: " + cloud);
        }
        if (StrUtil.hasBlank(ua, deviceId, filePath)) {
            return badRequest("ua、deviceId 和 filePath 不能为空");
        }
        if (size < 0) {
            return badRequest("size 不能小于 0");
        }
        return ResponseEntity.ok(Rsres.success(
                cloudUtil.getDownloadUrlOnCopy(cloudStorage, ua, deviceId, filePath, size)));
    }

    @GetMapping("/115/addFolder")
    public Rsres<Object> addFolder(@RequestParam String fileName) {
        return Rsres.success(r115Proxy.addFolder(fileName));
    }

    private CloudStorageType requireCloudStorage(String cloud) {
        return CloudStorageType.of(StrUtil.trim(cloud));
    }

    private ResponseEntity<Rsres<Object>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Rsres.fail(message));
    }
}
