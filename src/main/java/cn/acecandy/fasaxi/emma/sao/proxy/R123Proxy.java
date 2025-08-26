package cn.acecandy.fasaxi.emma.sao.proxy;

import cn.acecandy.fasaxi.emma.common.enums.CloudStorageType;
import cn.acecandy.fasaxi.emma.common.ex.BaseException;
import cn.acecandy.fasaxi.emma.sao.client.R123Client;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.dto.Rile;
import cn.acecandy.fasaxi.emma.sao.out.R123;
import cn.acecandy.fasaxi.emma.sao.out.R123DownloadUrlResp;
import cn.acecandy.fasaxi.emma.sao.out.R123FileListReq;
import cn.acecandy.fasaxi.emma.sao.out.R123FileListResp;
import cn.acecandy.fasaxi.emma.sao.out.R123TokenReq;
import cn.acecandy.fasaxi.emma.sao.out.R123TokenResp;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.collection.ListUtil;
import org.dromara.hutool.core.date.DateUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.core.text.UnicodeUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
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

    @Getter
    @Value("${cloud.r123.default.client-id}")
    private String clientId;

    @Getter
    @Value("${cloud.r123.default.client-secret}")
    private String clientSecret;

    @Resource
    private RedisClient redisClient;

    public String getCacheTokenKey() {
        return R_123_TOKEN;
    }

    /**
     * r123页数限制
     */
    private static final Integer PAGE_LIMIT = 100;

    /**
     * 获取访问令牌
     *
     * @return {@link R123TokenResp }
     */
    public R123TokenResp getAccessToken() {
        if (StrUtil.isBlank(getClientSecret()) || StrUtil.isBlank(getClientId())) {
            throw new BaseException(ERRCODE_1001);
        }
        R123<R123TokenResp> result = r123Client.getAccessToken(
                R123TokenReq.builder().clientID(getClientId()).clientSecret(getClientSecret()).build());
        if (result == null || !result.isOk()) {
            log.warn("getAccessToken,resp异常:{}", result);
            throw new BaseException(ERRCODE_1002 + ":获取令牌失败");
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
            r123Token = redisClient.getStr(getCacheTokenKey());
            if (StrUtil.isNotBlank(r123Token)) {
                return r123Token;
            }
        }
        R123TokenResp result = getAccessToken();
        r123Token = result.getAccessToken();
        int expire = (int) (OffsetDateTime.parse(result.getExpiredAt()).toEpochSecond()
                - DateUtil.currentSeconds() - 60 * 10);

        redisClient.set(getCacheTokenKey(), r123Token, expire);
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
            log.warn("getFileList,resp为空,req:{}", req);
            return null;
        }
        if (result.isOk()) {
            return result.getData();
        }
        if (isTokenError(result.getCode())) {
            redisClient.del(getCacheTokenKey());
        }
        log.warn("getFileList,resp异常:{},req:{}", result, req);
        return null;
    }

    private boolean isTokenError(Integer code) {
        return code == 401;
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
                    .parentFileId(parentId).limit(PAGE_LIMIT).lastFileId(lastFileId).build());

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
     * 获取Rile列表(精准搜索，此时文件id无效)
     *
     * @param filterFileName 筛选器文件名
     * @return {@link List }<{@link Rile }>
     */
    public List<Rile> listRiles(CharSequence filterFileName) {
        List<Rile> resultList = ListUtil.of();
        if (StrUtil.isBlank(filterFileName)) {
            return resultList;
        }
        Long lastFileId = null;
        do {
            R123FileListResp resp = getFileList(R123FileListReq.builder()
                    .parentFileId(0L).searchData(filterFileName.toString()).searchMode(1)
                    .limit(10).lastFileId(lastFileId).build());
            lastFileId = resp.getLastFileId();
            // 过滤非回收站文件，传入文件名则根据文件名筛选
            List<R123FileListResp.FileInfo> fileList = resp.getFileList().stream()
                    .filter(item -> item.getTrashed() == 0)
                    .filter(file -> StrUtil.equals(file.getFilename(), filterFileName))
                    .toList();
            if (CollUtil.isNotEmpty(fileList)) {
                resultList.addAll(convertToRile(fileList));
                break;
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
                    .fileName(UnicodeUtil.toString(f.getFilename()))
                    .fileSize(f.getSize())
                    .build();
            rile.setFileType(CloudStorageType.R_123, f.getType());
            return rile;
        }).toList();
    }


    /**
     * 获取文件列表
     *
     * @param fileId 文件id
     * @return {@link R123FileListResp }
     */
    public String getDownloadUrl(String ua, Long fileId) {
        String auth = getAccessTokenByCache(false);
        R123<R123DownloadUrlResp> result = r123Client.getDownloadUrl(auth, ua, fileId);
        if (result == null) {
            log.warn("getDownloadUrl,resp为空,fileId:{}", fileId);
            return null;
        }
        if (result.isOk()) {
            return result.getData().getDownloadUrl();
        }
        if (isTokenError(result.getCode())) {
            redisClient.del(getCacheTokenKey());
        }
        log.warn("getDownloadUrl,resp异常:{},fileId:{}", result, fileId);
        return null;
    }
}