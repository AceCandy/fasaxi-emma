package cn.acecandy.fasaxi.emma.service;

import cn.acecandy.fasaxi.emma.common.enums.EmbyPicType;
import cn.acecandy.fasaxi.emma.common.ex.BaseException;
import cn.acecandy.fasaxi.emma.config.DoubanConfig;
import cn.acecandy.fasaxi.emma.config.EmbyConfig;
import cn.acecandy.fasaxi.emma.config.EmbyContentCacheReqWrapper;
import cn.acecandy.fasaxi.emma.config.TmdbConfig;
import cn.acecandy.fasaxi.emma.dao.embyboss.entity.EmbyItemPic;
import cn.acecandy.fasaxi.emma.dao.embyboss.service.EmbyItemPicDao;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.client.RedisLockClient;
import cn.acecandy.fasaxi.emma.sao.out.EmbyRemoteImageOut;
import cn.acecandy.fasaxi.emma.sao.proxy.EmbyProxy;
import cn.acecandy.fasaxi.emma.utils.CacheUtil;
import cn.acecandy.fasaxi.emma.utils.ThreadUtil;
import cn.hutool.v7.core.map.MapUtil;
import cn.hutool.v7.core.math.NumberUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.http.client.engine.ClientEngine;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_204;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_404;
import static cn.acecandy.fasaxi.emma.common.enums.EmbyPicType.非图片;
import static cn.acecandy.fasaxi.emma.sao.client.RedisLockClient.buildPicLock;
import static cn.acecandy.fasaxi.emma.utils.EmbyProxyUtil.getCdnPicUrl;
import static cn.acecandy.fasaxi.emma.utils.EmbyProxyUtil.getPic302Uri;
import static cn.acecandy.fasaxi.emma.utils.EmbyProxyUtil.getPicUri;
import static cn.acecandy.fasaxi.emma.utils.EmbyProxyUtil.parseItemIdByUrl;

/**
 * 图片重定向服务
 *
 * @author AceCandy
 * @since 2024/11/19
 */
@Slf4j
@Component
public class PicRedirectService {

    @Resource
    private OriginReqService originReqService;

    @Resource
    private VirtualService virtualService;

    @Resource
    private EmbyProxy embyProxy;

    @Resource
    private EmbyItemPicDao embyItemPicDao;

    @Resource
    private TmdbConfig tmdbConfig;

    @Resource
    private DoubanConfig doubanConfig;

    @Resource
    private RedisClient redisClient;
    @Resource
    private EmbyConfig embyConfig;
    @Resource
    private ClientEngine httpClient;
    @Resource
    private RedisLockClient redisLockClient;

    /**
     * 处理图片重定向请求
     * // https://api.themoviedb.org/3/tv/1399/images?api_key=xxx
     * // https://static-mdb.v.geilijiasu.com/t/p/w400/xxx.png
     *
     * @param request  要求
     * @param response 响应
     */
    @SneakyThrows
    public void processPic(EmbyContentCacheReqWrapper request, HttpServletResponse response, EmbyPicType picType) {
        if (非图片.equals(picType)) {
            return;
        }
        String itemId = parseItemIdByUrl(request.getRequestURI());
        if (StrUtil.isBlank(itemId)) {
            response.setStatus(CODE_404);
            return;
        }
        if (Integer.parseInt(itemId) < 0) {
            virtualService.handleImage(request, response);
            return;
        }

        String maxWidth = MapUtil.getStr(request.getCachedParam(), "maxWidth");
        if (getByCacheOrDb(response, picType, itemId, maxWidth)) {
            return;
        }

        // 获取或创建对应的锁
        String lockKey = buildPicLock(itemId, picType);
        if (!redisLockClient.lock(lockKey)) {
            response.setStatus(CODE_204);
            return;
        }
        try {
            if (getByCacheOrDb(response, picType, itemId, maxWidth)) {
                return;
            }
            exec302Pic(request, response, itemId, picType, maxWidth);
        } finally {
            redisLockClient.unlock(lockKey);
        }
    }

    private boolean getByCacheOrDb(HttpServletResponse response, EmbyPicType picType,
                                   String itemId, String maxWidth) {
        if (getByCache(response, picType, itemId, maxWidth)) {
            return true;
        }
        return getByDb(response, picType, itemId, maxWidth);
    }

    private boolean getByDb(HttpServletResponse response, EmbyPicType picType,
                            String itemId, String maxWidth) {
        EmbyItemPic itemPic = embyItemPicDao.findByItemId(NumberUtil.parseInt(itemId));
        String uri = getPic302Uri(itemPic, picType);
        if (StrUtil.isNotBlank(uri)) {
            String url = getCdnPicUrl(uri, doubanConfig, tmdbConfig, maxWidth);
            originReqService.return308(response, url);
            log.info("{}-图片重定向(DB):[{}-{}] => {}", picType, itemId, maxWidth, url);
            redisClient.set(CacheUtil.buildPicCacheKey(itemId, picType), url, 2 * 24 * 60 * 60);
            return true;
        }
        return false;
    }

    private boolean getByCache(HttpServletResponse response, EmbyPicType picType,
                               String itemId, String maxWidth) {
        String uri = redisClient.getStr(CacheUtil.buildPicCacheKey(itemId, picType));
        if (StrUtil.isBlank(uri)) {
            return false;
        }
        if (StrUtil.equals(uri, "undefined")) {
            // 无效缓存直接返回200
            response.setStatus(CODE_404);
        } else {
            String url = getCdnPicUrl(uri, doubanConfig, tmdbConfig, maxWidth);
            originReqService.return308(response, url);
        }
        return true;
    }

    /**
     * 执行图片302
     *
     * @param request  要求
     * @param response 响应
     * @param itemId   项目id
     * @param picType  图片类型
     */
    private void exec302Pic(EmbyContentCacheReqWrapper request, HttpServletResponse response,
                            String itemId, EmbyPicType picType, String maxWidth) {
        EmbyRemoteImageOut.Img imageInfo = embyProxy.getRemoteImage(itemId, picType);
        if (null == imageInfo) {
            originReqService.return308to200(response, request.getParamUri());
            // originReqService.forwardOriReq(request, response);
            return;
        } else if (StrUtil.equals(imageInfo.getUrl(), "undefined")) {
            response.setStatus(CODE_404);
            redisClient.set(CacheUtil.buildPicCacheKey(String.valueOf(itemId), picType), "undefined", 60 * 60);
            return;
        }
        String uri = getPicUri(imageInfo.getUrl(), tmdbConfig);
        String url = getCdnPicUrl(uri, doubanConfig, tmdbConfig, maxWidth);
        if (StrUtil.isBlank(url)) {
            originReqService.forwardOriReq(request, response);
            return;
        }

        originReqService.return308(response, url);
        log.warn("{}-图片重定向(请求):[{}-{}] => {}", picType, itemId, maxWidth, url);
        asyncWriteItemPic(NumberUtil.parseInt(itemId), uri, picType);
    }

    /**
     * 异步写入 实体
     *
     * @param itemId  项目id
     * @param url     网址
     * @param picType 图片类型
     */
    private void asyncWriteItemPic(Integer itemId, String url, EmbyPicType picType) {
        ThreadUtil.execVirtual(() -> {
            try {
                switch (picType) {
                    case 封面 ->
                            embyItemPicDao.insertOrUpdate(EmbyItemPic.builder().itemId(itemId).posterPath(url).build());
                    case 背景图 ->
                            embyItemPicDao.insertOrUpdate(EmbyItemPic.builder().itemId(itemId).backdropPath(url).build());
                    case Logo ->
                            embyItemPicDao.insertOrUpdate(EmbyItemPic.builder().itemId(itemId).logoPath(url).build());
                    default -> throw new BaseException("图片类型异常: " + picType);
                }
            } finally {
                redisClient.set(CacheUtil.buildPicCacheKey(String.valueOf(itemId), picType), url, 2 * 24 * 60 * 60);
            }
        });
    }
}