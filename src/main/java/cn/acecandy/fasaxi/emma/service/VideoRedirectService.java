package cn.acecandy.fasaxi.emma.service;

import cn.acecandy.fasaxi.emma.config.EmbyConfig;
import cn.acecandy.fasaxi.emma.config.EmbyContentCacheReqWrapper;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.out.EmbyItemInfoOut;
import cn.acecandy.fasaxi.emma.sao.proxy.EmbyProxy;
import cn.acecandy.fasaxi.emma.utils.LockUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.date.DateUtil;
import org.dromara.hutool.core.map.MapUtil;
import org.dromara.hutool.core.net.url.UrlQueryUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_429;

/**
 * 视频重定向服务
 *
 * @author AceCandy
 * @since 2024/11/19
 */
@Slf4j
@Component
public class VideoRedirectService {

    @Resource
    private OriginReqService originReqService;

    @Resource
    private RedisClient redisClient;

    @Resource
    private EmbyConfig embyConfig;

    @Resource
    private EmbyProxy embyProxy;

    @SneakyThrows
    public void processVideo(EmbyContentCacheReqWrapper request, HttpServletResponse response) {
        String mediaSourceId = request.getMediaSourceId();
        if (StrUtil.isBlank(mediaSourceId)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String ua = request.getUa();
        String cacheKey = ua + "|" + mediaSourceId;
        String cacheUrl = redisClient.getStrFindOne(ua + "|" + mediaSourceId, mediaSourceId);
        if (StrUtil.isNotBlank(cacheUrl)) {
            cacheUrl = getPtUrl(cacheUrl);
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", cacheUrl);
            log.warn("重定向(缓存):[{}] => {}", cacheKey, cacheUrl);
            return;
        }
        // 获取或创建对应的锁
        Lock lock = LockUtil.lockVideo(mediaSourceId);
        if (LockUtil.isLock(lock)) {
            response.setStatus(CODE_429);
            return;
        }
        try {
            cacheUrl = redisClient.getStrFindOne(ua + "|" + mediaSourceId, mediaSourceId);
            if (StrUtil.isNotBlank(cacheUrl)) {
                cacheUrl = getPtUrl(cacheUrl);
                response.setStatus(HttpServletResponse.SC_FOUND);
                response.setHeader("Location", cacheUrl);
                log.warn("重定向(缓存):[{}] => {}", cacheKey, cacheUrl);
                return;
            }

            exec302(request, response, mediaSourceId);
        } finally {
            LockUtil.unlockVideo(lock, mediaSourceId);
        }
    }

    private String getPtUrl(String cacheUrl) {
        if (!StrUtil.startWithIgnoreCase(cacheUrl, embyConfig.getOriginPt())) {
            return cacheUrl;
        }
        int minute = DateUtil.thisMinute();
        if (minute == 0) {
            // 使用原始路径
        } else if (minute % 10 == 0) {
            cacheUrl = StrUtil.replaceIgnoreCase(cacheUrl,
                    embyConfig.getOriginPt(), embyConfig.getTransPt1());
        } else {
            if (minute % 3 == 0) {
                cacheUrl = StrUtil.replaceIgnoreCase(cacheUrl,
                        embyConfig.getOriginPt(), embyConfig.getTransPt2());
            } else if (minute % 3 == 1) {
                cacheUrl = StrUtil.replaceIgnoreCase(cacheUrl,
                        embyConfig.getOriginPt(), embyConfig.getTransPt3());
            } else if (minute % 3 == 2) {
                cacheUrl = StrUtil.replaceIgnoreCase(cacheUrl,
                        embyConfig.getOriginPt(), embyConfig.getTransPt4());
            }
        }
        return cacheUrl;
    }

    private void exec302(EmbyContentCacheReqWrapper request,
                         HttpServletResponse response, String mediaSourceId) {
        EmbyItemInfoOut.Item itemInfo = embyProxy.getItemInfo(mediaSourceId);
        if (null == itemInfo) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String mediaPath = itemInfo.getPath();
        Map<String, String> header302 = MapUtil.<String, String>builder()
                .put("User-Agent", request.getUa()).put("range", request.getRange()).build();
        // fileCacheUtil.cacheNextEpisode(embyInfo, header302);
        if (StrUtil.startWithIgnoreCase(mediaPath, "http")) {
            String realUrl = embyProxy.fetch302Path(mediaPath, header302);
            if (StrUtil.isBlank(realUrl)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            /*if (findRangeCache(request, response, embyInfo)) {
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                return;
            }*/

            int exTime = 2 * 24 * 60 * 60;
            if (StrUtil.startWithIgnoreCase(realUrl, embyConfig.getOriginPt())) {
                redisClient.set(mediaSourceId, realUrl, exTime);
            } else {
                exTime = (int) (MapUtil.getLong(UrlQueryUtil.decodeQuery(realUrl, Charset.defaultCharset()),
                        "t") - DateUtil.currentSeconds() - 5 * 60);
                redisClient.set(request.getUa() + "|" + mediaSourceId, realUrl, exTime);
            }

            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", realUrl);
            log.info("▲ 请求重定向->[{}-{}] {} 【{}】", response.getStatus(),
                    request.getMethod(), request.getRequestURI(), request.getCachedHeader());
            log.warn("重定向({}):[{}] => {}", DateUtil.date(DateUtil.currentSeconds() + exTime), mediaPath, realUrl);
            return;
        } else {
            // TODO 这里是不需要302的本地路径
        }
    }
}