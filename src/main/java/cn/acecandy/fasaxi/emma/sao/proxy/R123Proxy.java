package cn.acecandy.fasaxi.emma.sao.proxy;

import cn.acecandy.fasaxi.emma.common.ex.BaseException;
import cn.acecandy.fasaxi.emma.sao.client.R123Client;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.out.R123;
import cn.acecandy.fasaxi.emma.sao.out.R123FileListReq;
import cn.acecandy.fasaxi.emma.sao.out.R123FileListResp;
import cn.acecandy.fasaxi.emma.sao.out.R123TokenReq;
import cn.acecandy.fasaxi.emma.sao.out.R123TokenResp;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.text.StrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static cn.acecandy.fasaxi.emma.common.enums.ErrCode.ERRCODE_1001;
import static cn.acecandy.fasaxi.emma.common.enums.ErrCode.ERRCODE_1002;

/**
 * r123代理
 *
 * @author tangningzhu
 * @since 2025/8/18
 */
@Component
@Slf4j
public class R123Proxy {

    @Resource
    private R123Client r123Client;

    @Value("${cloud.r123.client-id}")
    private String clientId;

    @Value("${cloud.r123.client-secret}")
    private String clientSecret;

    @Resource
    private RedisClient redisClient;

    /**
     * 获取访问令牌
     *
     * @return {@link R123TokenResp }
     */
    public R123TokenResp getAccessToken() {
        if (StrUtil.isBlank(clientSecret) || StrUtil.isBlank(clientId)) {
            throw new BaseException(ERRCODE_1001);
        }
        R123<R123TokenResp> result = r123Client
                .getAccessToken(R123TokenReq.builder().clientID(clientId).clientSecret(clientSecret).build());
        if (result == null || !result.isOk()) {
            throw new BaseException(ERRCODE_1002,
                    StrUtil.format(" getAccessToken,resp:{}", result));
        }
        return result.getData();
    }

    /**
     * 获取文件列表
     *
     * @param req 请求入参
     * @return {@link R123FileListResp }
     */
    public R123FileListResp getFileList(R123FileListReq req) {
        // String auth = getAccessToken().getAccessToken();
        String auth = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3NTYxMTU1ODQsImlhdCI6MTc1NTUxMDc4NCwiaWQiOjE4MjEzNzM4ODAsIm1haWwiOiIiLCJuaWNrbmFtZSI6IjE3NjIzMzA4MDY1IiwidXNlcm5hbWUiOjE3NjIzMzA4MDY1LCJ2IjowfQ.CBOSH9W6ZYKeiEPASDdK5uQinzquMQvg96kZUngKQ7k";
        R123<R123FileListResp> result = r123Client.getFileList(auth, req);
        if (result == null || !result.isOk()) {
            throw new BaseException(ERRCODE_1002,
                    StrUtil.format(" getFileList,resp:{}", result));
        }
        return result.getData();
    }

}