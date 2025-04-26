package cn.acecandy.fasaxi.emma.control;

import cn.acecandy.fasaxi.emma.common.resp.Rsres;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "健康检查")
@RestController
@RequestMapping("/health")
public class HealthController {

    @Operation(summary = "当前系统时间")
    @GetMapping("/time")
    public Rsres<Object> health() {
        return Rsres.success(System.currentTimeMillis());
    }

}