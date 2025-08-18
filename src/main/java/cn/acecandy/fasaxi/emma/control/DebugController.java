package cn.acecandy.fasaxi.emma.control;

import cn.acecandy.fasaxi.emma.common.resp.Rsres;
import cn.acecandy.fasaxi.emma.sao.out.R123FileListReq;
import cn.acecandy.fasaxi.emma.sao.proxy.R123Proxy;
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
    private R123Proxy r123Proxy;

    @GetMapping("/123/getAccessToken")
    public Rsres<Object> getAccessToken() {
        return Rsres.success(r123Proxy.getAccessToken());
    }

    @GetMapping("/123/getFileList")
    public Rsres<Object> getFileList(R123FileListReq req) {
        return Rsres.success(r123Proxy.getFileList(req));
    }
}