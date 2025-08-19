package cn.acecandy.fasaxi.emma.sao.proxy;

import cn.acecandy.fasaxi.emma.common.enums.CloudStorageType;
import cn.acecandy.fasaxi.emma.common.ex.BaseException;
import cn.acecandy.fasaxi.emma.sao.client.R123Client;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.dto.Rile;
import cn.acecandy.fasaxi.emma.sao.out.R123;
import cn.acecandy.fasaxi.emma.sao.out.R123FileListReq;
import cn.acecandy.fasaxi.emma.sao.out.R123FileListResp;
import cn.acecandy.fasaxi.emma.sao.out.R123TokenReq;
import cn.acecandy.fasaxi.emma.sao.out.R123TokenResp;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.collection.ListUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.core.util.ObjUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

import static cn.acecandy.fasaxi.emma.common.enums.ErrCode.ERRCODE_1001;
import static cn.acecandy.fasaxi.emma.common.enums.ErrCode.ERRCODE_1002;
import static cn.acecandy.fasaxi.emma.utils.CacheUtil.R_123_TOKEN;

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
     * r123页数限制
     */
    private static final Integer R123_PAGE_LIMIT = 100;

    /**
     * 获取访问令牌
     *
     * @return {@link R123TokenResp }
     */
    private R123TokenResp getAccessToken() {
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
     * 获取访问令牌(缓存)
     *
     * @return {@link R123TokenResp }
     */
    public String getAccessTokenByCache(boolean force) {
        String r123Token = "";
        if (!force) {
            r123Token = redisClient.getStr(R_123_TOKEN);
            if (StrUtil.isNotBlank(r123Token)) {
                return r123Token;
            }
        }
        R123TokenResp result = getAccessToken();
        r123Token = result.getAccessToken();
        // 缓存20天
        redisClient.set(R_123_TOKEN, r123Token, 60 * 60 * 24 * 20);
        return r123Token;
    }

    /**
     * 获取文件列表
     *
     * @param req 请求入参
     * @return {@link R123FileListResp }
     */
    public R123FileListResp getFileList(R123FileListReq req) {
        String auth = getAccessTokenByCache(false);
        R123<R123FileListResp> result = r123Client.getFileList(auth, req);
        if (result == null) {
            throw new BaseException(ERRCODE_1002,
                    StrUtil.format(" getFileList,resp为空"));
        }
        if (result.isOk()) {
            return result.getData();
        }
        if (ObjUtil.equals(result.getCode(), 401)) {
            redisClient.del(R_123_TOKEN);
        }
        throw new BaseException(ERRCODE_1002,
                StrUtil.format(" getFileList,resp:{}", result));
    }

    /**
     * 获取Rile列表
     *
     * @param parentId       父ID
     * @param filterFileName 筛选器文件名
     * @return {@link List }<{@link Rile }>
     */
    public List<Rile> listRiles(Long parentId, CharSequence filterFileName) {
        List<Rile> resultList = ListUtil.of();
        Long lastFileId = null;
        do {
            R123FileListResp resp = getFileList(R123FileListReq.builder()
                    .parentFileId(parentId).limit(R123_PAGE_LIMIT).lastFileId(lastFileId).build());

            lastFileId = resp.getLastFileId();
            // 过滤非回收站文件，传入文件名则根据文件名筛选
            List<R123FileListResp.FileInfo> fileList = resp.getFileList().stream()
                    .filter(item -> item.getTrashed() == 0)
                    .filter(file -> StrUtil.isBlank(filterFileName) ||
                            StrUtil.equals(file.getFilename(), filterFileName))
                    .toList();
            if (CollUtil.isNotEmpty(fileList)) {
                resultList.addAll(convertToRile(fileList));
                // 如果是按文件名筛选且已找到，直接退出循环
                if (StrUtil.isNotBlank(filterFileName)) {
                    break;
                }
            }
        } while (lastFileId != -1);

        return resultList;
    }

    /**
     * 转化为Rile
     *
     * @param files 文件
     * @return {@link List }<{@link Rile }>
     */
    private List<Rile> convertToRile(List<R123FileListResp.FileInfo> files) {
        return files.stream().map(f -> {
            Rile rile = Rile.builder().fileId(f.getFileId())
                    .fileName(f.getFilename())
                    .fileSize(f.getSize())
                    .build();
            rile.setFileType(CloudStorageType.R_123, f.getType());
            return rile;
        }).toList();
    }
}