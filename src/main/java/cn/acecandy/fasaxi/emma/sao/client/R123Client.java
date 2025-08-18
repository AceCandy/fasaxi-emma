package cn.acecandy.fasaxi.emma.sao.client;

import cn.acecandy.fasaxi.emma.sao.config.LogFeignConfig;
import cn.acecandy.fasaxi.emma.sao.out.R123;
import cn.acecandy.fasaxi.emma.sao.out.R123TokenReq;
import cn.acecandy.fasaxi.emma.sao.out.R123TokenResp;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 123 client
 *
 * @author tangningzhu
 * @since 2025/8/15
 */
@FeignClient(name = "R123Client", url = "${cloud.r123.host}",
        configuration = LogFeignConfig.class)
public interface R123Client {

    // 声明要调用的服务接口，与服务提供者的接口定义一致
    // @GetMapping("/users/{id}")
    // User getUserById(@PathVariable("id") Long id);
    //
    // @GetMapping("/users")
    // List<User> getAllUsers();

    /**
     * 获取访问令牌
     *
     * @param req 请求入参
     * @return {@link R123 }<{@link R123TokenResp }>
     */
    @PostMapping(path = "/api/v1/access_token", headers = {"Platform=open_platform"},
            consumes = {MediaType.APPLICATION_JSON_VALUE})
    R123<R123TokenResp> getAccessToken(@RequestBody R123TokenReq req);

}