package cn.acecandy.fasaxi.emma.sao.proxy;

import cn.acecandy.fasaxi.emma.common.enums.CloudStorageType;
import cn.acecandy.fasaxi.emma.common.ex.BaseException;
import cn.acecandy.fasaxi.emma.sao.client.R115AuthClient;
import cn.acecandy.fasaxi.emma.sao.client.R115Client;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.dto.Rile;
import cn.acecandy.fasaxi.emma.sao.out.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.collection.ListUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.core.text.UnicodeUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static cn.acecandy.fasaxi.emma.common.enums.ErrCode.ERRCODE_1101;
import static cn.acecandy.fasaxi.emma.common.enums.ErrCode.ERRCODE_1102;
import static cn.acecandy.fasaxi.emma.utils.CacheUtil.R_115_REFRESH_TOKEN;
import static cn.acecandy.fasaxi.emma.utils.CacheUtil.R_115_TOKEN;

/**
 * r115代理
 *
 * @author tangningzhu
 * @since 2025/8/18
 */
@Component
@Slf4j
public class R115Proxy {

    @Resource
    private R115Client r115Client;

    @Resource
    private R115AuthClient r115AuthClient;

    @Value("${cloud.r115.default.refresh-token}")
    private String refreshToken;

    public String getRefreshToken() {
        String result = redisClient.getStr(R_115_REFRESH_TOKEN);
        if (StrUtil.isNotBlank(result)) {
            return result;
        }
        return refreshToken;
    }

    @Resource
    private RedisClient redisClient;

    public String getCacheTokenKey() {
        return R_115_TOKEN;
    }

    /**
     * 页数限制
     */
    private static final Integer PAGE_LIMIT = 1150;

    /**
     * 获取访问令牌
     *
     * @return {@link R123TokenResp }
     */
    public R115RefreshTokenResp refreshToken() {
        String refreshToken = getRefreshToken();
        if (StrUtil.isBlank(refreshToken)) {
            throw new BaseException(ERRCODE_1101);
        }
        R115Auth<R115RefreshTokenResp> result = r115AuthClient.refreshToken(
                R115RefreshTokenReq.builder().refresh_token(refreshToken).build());
        if (result == null || !result.isOk()) {
            log.warn("refreshToken,resp异常:{}", result);
            throw new BaseException(ERRCODE_1102 + "");
        }
        refreshToken = result.getData().getRefreshToken();
        redisClient.set(R_115_REFRESH_TOKEN, refreshToken);
        return result.getData();
    }

    /**
     * 获取访问令牌(缓存)
     * <p>
     * 加上Bearer前缀
     *
     * @return {@link R123TokenResp }
     */
    public String getAccessTokenByCache(boolean force) {
        String r115Token = "";
        if (!force) {
            r115Token = redisClient.getStr(getCacheTokenKey());
            if (StrUtil.isNotBlank(r115Token)) {
                return "Bearer " + r115Token;
            }
        }
        R115RefreshTokenResp result = refreshToken();
        r115Token = result.getAccessToken();
        int expire = (int) (result.getExpiresIn() - 60 * 2);

        redisClient.set(getCacheTokenKey(), r115Token, expire);
        return "Bearer " + r115Token;
    }

    /**
     * 获取文件列表
     *
     * @param req 请求入参
     * @return {@link R123FileListResp }
     */
    public R115Search<List<R115SearchFileResp>> searchFile(R115SearchFileReq req) {
        String auth = getAccessTokenByCache(false);
        R115Search<List<R115SearchFileResp>> result = r115Client.searchFile(auth, req);
        if (result == null) {
            log.warn("searchFile,resp为空,req:{}", req);
            return null;
        }
        if (result.isOk()) {
            return result;
        }
        if (isTokenError(result.getCode())) {
            redisClient.del(getCacheTokenKey());
        }
        log.warn("searchFile,resp异常:{},req:{}", result, req);
        return null;
    }

    /**
     * 获取文件列表
     *
     * @param req 请求入参
     * @return {@link R123FileListResp }
     */
    public R115File<List<R115FileListResp>> getFileList(R115FileListReq req) {
        String auth = getAccessTokenByCache(false);
        R115File<List<R115FileListResp>> result = r115Client.getFileList(auth, req);
        if (result == null) {
            log.warn("getFileList,resp为空,req:{}", req);
            return null;
        }
        if (result.isOk()) {
            return result;
        }
        if (isTokenError(result.getCode())) {
            redisClient.del(getCacheTokenKey());
        }
        log.warn("getFileList,resp异常:{},req:{}", result, req);
        return null;
    }

    /**
     * 获取文件信息
     *
     * @param req 请求入参
     * @return {@link R123FileListResp }
     */
    public R115FileInfoResp getFileInfo(Long fileId, R115FileInfoReq req) {
        String auth = getAccessTokenByCache(false);
        R115<R115FileInfoResp> result = r115Client.getFileInfo(auth, fileId, req);
        if (result == null) {
            log.warn("getFileInfo,resp为空,req:{}", req);
            return null;
        }
        if (result.isOk()) {
            return result.getData();
        }
        if (isTokenError(result.getCode())) {
            redisClient.del(getCacheTokenKey());
        }
        log.warn("getFileInfo,resp异常:{},req:{}", result, req);
        return null;
    }

    public List<Rile> listRiles(Long parentId, CharSequence filterFileName) {
        List<Rile> resultList = ListUtil.of();
        int offset = 0;
        boolean hasMore;

        do {
            R115File<List<R115FileListResp>> result = getFileList(R115FileListReq.builder()
                    .cid(parentId).limit(PAGE_LIMIT).offset(offset).show_dir(1).cur(1).build());

            // 过滤非回收站文件，传入文件名则根据文件名筛选
            List<R115FileListResp> fileList = result.getData().stream()
                    .filter(item -> StrUtil.equals(item.getAid(), "1"))
                    .filter(file -> StrUtil.isBlank(filterFileName) ||
                            StrUtil.equals(file.getFn(), filterFileName))
                    .toList();
            if (CollUtil.isNotEmpty(fileList)) {
                resultList.addAll(convertToRile(fileList));
                // 如果是按文件名筛选且已找到，直接退出循环
                if (StrUtil.isNotBlank(filterFileName)) {
                    break;
                }
            }
            // 更新分页信息
            offset += result.getCount();
            hasMore = result.getCount() < PAGE_LIMIT;
        } while (hasMore);

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
        int offset = 0;
        boolean hasMore;
        do {
            R115Search<List<R115SearchFileResp>> resp = searchFile(R115SearchFileReq.builder()
                    .search_value(filterFileName.toString()).offset(offset).fc(2).type(4)
                    .limit(10).build());
            if (null == resp) {
                return resultList;
            }
            // 过滤非回收站文件，传入文件名则根据文件名筛选
            List<R115SearchFileResp> fileList = resp.getData().stream()
                    .filter(item -> StrUtil.equals(item.getArea_id(), "1"))
                    .filter(file -> StrUtil.equals(
                            UnicodeUtil.toString(file.getFile_name()), filterFileName))
                    .toList();
            if (CollUtil.isNotEmpty(fileList)) {
                resultList.addAll(convertSearchToRile(fileList));
                break;
            }
            offset += resp.getCount();
            hasMore = resp.getCount() < PAGE_LIMIT;
        } while (hasMore);

        return resultList;
    }

    /**
     * 获取Rile信息(精准获取)
     *
     * @param filterFileName 筛选器文件名
     * @return {@link List }<{@link Rile }>
     */
    public List<Rile> getRiles(CharSequence filterFileName) {
        List<Rile> resultList = ListUtil.of();
        R115FileInfoResp resp = getFileInfo(null, R115FileInfoReq.builder().path(filterFileName.toString()).build());
        if (null == resp) {
            return resultList;
        }
        resultList.add(convertInfoToRile(resp));
        return resultList;
    }

    /**
     * 令牌错误判断辅助方法
     *
     * @param code 代码
     * @return boolean
     */
    private boolean isTokenError(Integer code) {
        return code >= 40140123 && code <= 40140126;
    }

    /**
     * 转化为Rile
     *
     * @param files 文件
     * @return {@link List }<{@link Rile }>
     */
    private List<Rile> convertSearchToRile(List<R115SearchFileResp> files) {
        return files.stream().map(f -> {
            Rile rile = Rile.builder().fileId(f.getFile_id())
                    .fileName(f.getFile_name())
                    .fileSize(f.getFile_size())
                    .pickCode(f.getPick_code())
                    .build();
            rile.setFileType(CloudStorageType.R_115, f.getFile_category());
            return rile;
        }).toList();
    }

    /**
     * 转化为Rile
     *
     * @param files 文件
     * @return {@link List }<{@link Rile }>
     */
    private List<Rile> convertToRile(List<R115FileListResp> files) {
        return files.stream().map(f -> {
            Rile rile = Rile.builder().fileId(f.getFid())
                    .fileName(f.getFn())
                    .fileSize(f.getFs())
                    .pickCode(f.getPc())
                    .build();
            rile.setFileType(CloudStorageType.R_115, f.getFc());
            return rile;
        }).toList();
    }


    /**
     * 转化为Rile
     *
     * @param file 文件
     * @return {@link List }<{@link Rile }>
     */
    private Rile convertInfoToRile(R115FileInfoResp file) {
        return Rile.builder().fileId(file.getFile_id())
                .fileName(file.getFile_name())
                .fileSize(file.getSize_byte())
                .pickCode(file.getPick_code())
                .build();
    }


    /**
     * 获取文件列表
     *
     * @param pickCode 选择代码
     * @return {@link R123FileListResp }
     */
    public String getDownloadUrl(String ua, String pickCode) {
        if (StrUtil.isBlank(pickCode)) {
            return null;
        }
        String auth = getAccessTokenByCache(false);
        R115<Map<Long, R115DownloadUrlResp>> result = r115Client.getDownloadUrl(auth, ua,
                R115DownloadUrlReq.builder().pick_code(pickCode).build());
        if (result == null) {
            log.warn("getDownloadUrl,resp为空,pickCode:{}", pickCode);
            return null;
        }
        if (result.isOk()) {
            return CollUtil.getFirst(result.getData().values()).getUrl().getUrl();
        }
        if (isTokenError(result.getCode())) {
            redisClient.del(getCacheTokenKey());
        }
        log.warn("getDownloadUrl,resp异常:{},pickCode:{}", result, pickCode);
        return null;
    }
}