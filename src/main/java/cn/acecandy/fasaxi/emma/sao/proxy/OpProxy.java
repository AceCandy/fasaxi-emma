package cn.acecandy.fasaxi.emma.sao.proxy;

import cn.acecandy.fasaxi.emma.common.ex.BaseException;
import cn.acecandy.fasaxi.emma.config.OpConfig;
import cn.acecandy.fasaxi.emma.sao.client.OpClient;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.client.RedisLockClient;
import cn.acecandy.fasaxi.emma.sao.entity.Op;
import cn.acecandy.fasaxi.emma.sao.out.R123TokenResp;
import cn.acecandy.fasaxi.emma.utils.ThreadUtil;
import cn.hutool.v7.core.text.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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

    private static final int TOKEN_EXPIRE_SECONDS = 60 * 60 * 36; // 36小时
    private static final Integer TOKEN_ERROR_CODE = 401;
    private static final int LOCK_EXPIRE_SECONDS = 10; // 分布式锁过期时间

    @Resource
    private OpClient opClient;

    @Resource
    private OpConfig opConfig;

    @Resource
    private RedisClient redisClient;

    @Resource
    private RedisLockClient redisLockClient;

    public String getCacheTokenKey() {
        return OPENLIST_TOKEN;
    }

    /**
     * 获取访问令牌
     *
     * @return {@link R123TokenResp }
     */
    public String login() {
        String username = opConfig.getUsername();
        Op<Op.LoginResp> result = opClient.login(
                new Op.LoginReq(username, opConfig.getPassword()));

        if (result == null || !result.isOk()) {
            log.warn("Login failed for user: {}, resp: {}", username, result);
            throw new BaseException(ERRCODE_1502);
        }
        String token = result.data().token();
        redisClient.set(getCacheTokenKey(), token, TOKEN_EXPIRE_SECONDS);
        log.info("Login successful for user: {}, token cached", username);
        return token;
    }

    /**
     * 获取令牌(缓存)
     *
     * @return {@link String }
     */
    public String getTokenByCache() {
        String token = redisClient.getStr(getCacheTokenKey());
        if (StrUtil.isNotBlank(token)) {
            return token;
        }
        return loginWithLock();
    }

    /**
     * 带分布式锁的登录方法，防止并发场景下重复登录
     *
     * @return {@link String }
     */
    private String loginWithLock() {
        String lockKey = getCacheTokenKey() + ":lock";

        // 尝试获取分布式锁
        if (redisLockClient.lock(lockKey, LOCK_EXPIRE_SECONDS)) {
            try {
                // 双重检查，防止锁竞争导致的重复登录
                String token = redisClient.getStr(getCacheTokenKey());
                if (StrUtil.isNotBlank(token)) {
                    return token;
                }
                return login();
            } finally {
                redisLockClient.unlock(lockKey);
            }
        } else {
            // 获取锁失败，等待100ms后重试
            ThreadUtil.safeSleep(100);
            return getTokenByCache();
        }
    }

    /**
     * 统一处理API响应结果
     *
     * @param result    API响应结果
     * @param req       请求对象
     * @param operation 操作名称
     * @param <T>       响应数据类型
     * @return 处理后的数据，如果失败返回null
     */
    private <T> T handleResponse(Op<T> result, Object req, String operation) {
        if (result == null) {
            log.error("{} failed: response is null, req: {}", operation, req);
            throw new BaseException(ERRCODE_1502);
        }
        if (result.isOk()) {
            return result.data();
        }
        if (isTokenError(result.code())) {
            redisClient.del(getCacheTokenKey());
        }
        return null;
    }

    /**
     * 处理返回void类型的响应
     *
     * @param result    API响应结果
     * @param req       请求对象
     * @param operation 操作名称
     * @return boolean 操作是否成功
     */
    private boolean handleVoidResponse(Op<Void> result, Object req, String operation) {
        if (result == null) {
            log.error("{} failed: response is null, req: {}", operation, req);
            return false;
        }
        if (result.isOk()) {
            return true;
        }
        if (isTokenError(result.code())) {
            redisClient.del(getCacheTokenKey());
        }
        return false;
    }

    /**
     * 创建目录(支持递归创建)
     *
     * @param path 路径
     * @return boolean
     */
    public boolean mkdir(String path) {
        if (StrUtil.isBlank(path)) {
            log.warn("mkdir failed: path cannot be empty");
            return false;
        }

        Op.MkdirReq req = new Op.MkdirReq(path);
        Op<Void> result = opClient.mkdir(getTokenByCache(), req);
        return handleVoidResponse(result, req, "mkdir");
    }

    /**
     * 重命名
     *
     * @param path 路径
     * @param name 名字
     * @return boolean
     */
    public boolean rename(String path, String name) {
        if (StrUtil.isBlank(path) || StrUtil.isBlank(name)) {
            log.warn("rename failed: path or name cannot be empty, path: {}, name: {}", path, name);
            return false;
        }

        Op.RenameReq req = new Op.RenameReq(path, name);
        Op<Void> result = opClient.rename(getTokenByCache(), req);
        return handleVoidResponse(result, req, "rename");
    }

    /**
     * 获取文件列表
     *
     * @param path    路径
     * @param refresh 刷新
     * @return {@link Op.ListResp }
     */
    public List<Op.ListContent> getFileList(String path, Boolean refresh) {
        if (StrUtil.isBlank(path)) {
            log.warn("getFileList failed: path cannot be empty");
            return null;
        }

        Op.ListReq req = new Op.ListReq(path, null, null, null, refresh);
        Op<Op.ListResp> result = opClient.list(getTokenByCache(), req);
        Op.ListResp data = handleResponse(result, req, "getFileList");
        return data != null ? data.content() : null;
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
        if (StrUtil.isBlank(srcDir) || StrUtil.isBlank(dstDir) || names == null || names.isEmpty()) {
            log.warn("copy failed: parameters invalid, srcDir: {}, dstDir: {}, names: {}", srcDir, dstDir, names);
            return false;
        }

        Op.CopyReq req = new Op.CopyReq(srcDir, dstDir, names, true);
        Op<Void> result = opClient.copy(getTokenByCache(), req);
        return handleVoidResponse(result, req, "copy");
    }

    /**
     * 删除文件
     *
     * @param dir   目录
     * @param names 文件名
     * @return boolean
     */
    public boolean remove(String dir, List<String> names) {
        if (StrUtil.isBlank(dir) || names == null || names.isEmpty()) {
            log.warn("remove failed: parameters invalid, dir: {}, names: {}", dir, names);
            return false;
        }

        Op.RemoveReq req = new Op.RemoveReq(dir, names);
        Op<Void> result = opClient.remove(getTokenByCache(), req);
        return handleVoidResponse(result, req, "remove");
    }

    /**
     * 令牌错误判断辅助方法
     *
     * @param code 代码
     * @return boolean
     */
    private boolean isTokenError(Integer code) {
        return TOKEN_ERROR_CODE.equals(code);
    }

}