package cn.acecandy.fasaxi.emma.control;

import cn.acecandy.fasaxi.emma.service.FastEmbyService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Emby处理入口")
@RestController
@RequestMapping("/emby")
public class FastEmbyController {

    @Resource
    private FastEmbyService fastEmbyService;

    /*@Operation(summary = "登录鉴权")
    @PostMapping("/authenticatebyname")
    public ResponseEntity<?> authenticateByName(@RequestBody EmbyAuthReq req,
                                                HttpServletRequest httpRequest) {
        // 1. 用户身份验证
        if (!StrUtil.equals(req.getUsername(), "AceCandy")) {
            log.warn("Username mismatch: {}", req.getUsername());
            return ResponseEntity.status(401)
                    .body(Map.of("message", "用户名不存在"));
        }

        // 2. 密码验证
        if (!StrUtil.equals(req.getUsername(), "123456")) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "密码错误"));
        }
        return null;
    }*/
/*
    @Operation(summary = "emby请求")
    @GetMapping({"videos/{videoId}/**", "Videos/{videoId}/**"})
    public ResponseEntity<?> handleEmbyRequest(@PathVariable String videoId, HttpServletRequest request,
                                               VideoRedirectReq req) {
        log.info("收到请求: [{}]{}, 提取的参数:{}", request.getMethod(),
                request.getRequestURI(), request.getParameterMap());
        return fastEmbyService.handleEmbyRequest(EmbyUtil.parseHttpReq(request), videoId, req);
    }*/

}