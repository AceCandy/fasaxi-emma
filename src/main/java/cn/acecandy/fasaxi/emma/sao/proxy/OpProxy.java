package cn.acecandy.fasaxi.emma.sao.proxy;

import cn.acecandy.fasaxi.emma.common.ex.BaseException;
import cn.acecandy.fasaxi.emma.config.OpConfig;
import cn.acecandy.fasaxi.emma.sao.client.OpClient;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.entity.Op;
import cn.acecandy.fasaxi.emma.sao.out.R123TokenResp;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.text.StrUtil;
import org.springframework.stereotype.Component;

import java.util.List;

import static cn.acecandy.fasaxi.emma.common.enums.ErrCode.ERRCODE_1502;
import static cn.acecandy.fasaxi.emma.utils.CacheUtil.OPENLIST_TOKEN;

/**
 * openlist 代理
 *
 * @author tangningzhu
 * @since 2025/8/18
 */
@Component
@Slf4j
public class OpProxy {

    @Resource
    private OpClient opClient;

    @Resource
    private OpConfig opConfig;

    @Resource
    private RedisClient redisClient;

    public String getCacheTokenKey() {
        return OPENLIST_TOKEN;
    }

    /**
     * 获取访问令牌
     *
     * @return {@link R123TokenResp }
     */
    public String login() {
        Op<Op.LoginResp> result = opClient.login(
                new Op.LoginReq(opConfig.getUsername(), opConfig.getPassword()));
        if (result == null || !result.isOk()) {
            log.warn("login,resp异常:{}", result);
            throw new BaseException(ERRCODE_1502);
        }

        String token = result.data().token();
        redisClient.set(getCacheTokenKey(), token, 60 * 60 * 36);
        return token;
    }


    /**
     * 获取令牌(缓存)
     *
     * @return {@link String }
     */
    public String getTokenByCache() {
        String result = redisClient.getStr(getCacheTokenKey());
        if (StrUtil.isNotBlank(result)) {
            return result;
        }
        return login();
    }

    /**
     * 创建目录(支持递归创建)
     *
     * @param path 路径
     * @return boolean
     */
    public boolean mkdir(String path) {
        Op.MkdirReq req = new Op.MkdirReq(path);
        Op<Void> result = opClient.mkdir(getTokenByCache(), req);
        if (result == null) {
            throw new BaseException(ERRCODE_1502);
        }
        if (result.isOk()) {
            return true;
        }
        if (isTokenError(result.code())) {
            redisClient.del(getCacheTokenKey());
        }
        log.warn("mkdir,resp异常:{},req:{}", result, req);
        return false;
    }

    /**
     * 重命名
     *
     * @param path 路径
     * @param name 名字
     * @return boolean
     */
    public boolean rename(String path, String name) {
        Op.RenameReq req = new Op.RenameReq(path, name);
        Op<Void> result = opClient.rename(getTokenByCache(), req);
        if (result == null) {
            throw new BaseException(ERRCODE_1502);
        }
        if (result.isOk()) {
            return true;
        }
        if (isTokenError(result.code())) {
            redisClient.del(getCacheTokenKey());
        }
        log.warn("rename,resp异常:{},req:{}", result, req);
        return false;
    }

    /**
     * 获取文件列表
     *
     * @param path    路径
     * @param refresh 刷新
     * @return {@link Op.ListResp }
     */
    public List<Op.ListContent> getFileList(String path, Boolean refresh) {
        Op.ListReq req = new Op.ListReq(path, null, null, null, refresh);
        Op<Op.ListResp> result = opClient.list(getTokenByCache(), req);
        if (result == null) {
            throw new BaseException(ERRCODE_1502);
        }
        if (result.isOk()) {
            return result.data().content();
        }
        if (isTokenError(result.code())) {
            redisClient.del(getCacheTokenKey());
        }
        log.warn("getFileList,resp异常:{},req:{}", result, req);
        return null;
    }

    /**
     * 复制文件
     *
     * @param srcDir src目录
     * @param dstDir dst目录
     * @param names  名字
     * @return boolean
     */
    public boolean copy(String srcDir, String dstDir, List<String> names) {
        Op.CopyReq req = new Op.CopyReq(srcDir, dstDir, names, true);
        Op<Void> result = opClient.copy(getTokenByCache(), req);
        if (result == null) {
            throw new BaseException(ERRCODE_1502);
        }
        if (result.isOk()) {
            return true;
        }
        if (isTokenError(result.code())) {
            redisClient.del(getCacheTokenKey());
        }
        log.warn("copy,resp异常:{},req:{}", result, req);
        return false;
    }

    /**
     * 删除文件
     *
     * @param dir   目录
     * @param names 文件名
     * @return boolean
     */
    public boolean remove(String dir, List<String> names) {
        Op.RemoveReq req = new Op.RemoveReq(dir, names);
        Op<Void> result = opClient.remove(getTokenByCache(), req);
        if (result == null) {
            throw new BaseException(ERRCODE_1502);
        }
        if (result.isOk()) {
            return true;
        }
        if (isTokenError(result.code())) {
            redisClient.del(getCacheTokenKey());
        }
        log.warn("remove,resp异常:{},req:{}", result, req);
        return false;
    }

    /**
     * 令牌错误判断辅助方法
     *
     * @param code 代码
     * @return boolean
     */
    private boolean isTokenError(Integer code) {
        return code == 401;
    }

}