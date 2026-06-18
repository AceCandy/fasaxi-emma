package cn.acecandy.fasaxi.emma.config;

import cn.acecandy.fasaxi.emma.common.enums.EmbyPicType;
import cn.acecandy.fasaxi.emma.service.OriginReqService;
import cn.acecandy.fasaxi.emma.service.PicRedirectService;
import cn.acecandy.fasaxi.emma.service.VideoRedirectService;
import cn.acecandy.fasaxi.emma.service.VirtualService;
import cn.acecandy.fasaxi.emma.utils.LogSanitizer;
import cn.acecandy.fasaxi.emma.utils.ReUtil;
import cn.hutool.v7.core.date.DateUtil;
import cn.hutool.v7.core.text.StrUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;

import java.io.IOException;

import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_416;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_504;
import static cn.acecandy.fasaxi.emma.common.enums.EmbyPicType.非图片;
import static cn.acecandy.fasaxi.emma.service.VirtualService.isMimickedId;
import static cn.acecandy.fasaxi.emma.utils.EmbyProxyUtil.getPicType;
import static cn.acecandy.fasaxi.emma.utils.EmbyProxyUtil.needClose;

/**
 * 转发所有emby请求
 *
 * @author tangningzhu
 * @since 2025/4/15
 */
@Slf4j
@Order(2)
@WebFilter("/*")
public class EmbyProxyFilter implements Filter {

    @Resource
    private AccessLog accessLog;

    @Resource
    private PicRedirectService picRedirectService;

    @Resource
    private VideoRedirectService videoRedirectService;

    @Resource
    private OriginReqService originReqService;

    @Resource
    private VirtualService virtualService;


    @Override
    @SneakyThrows
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        long start = DateUtil.current();
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        if (StrUtil.startWith(req.getRequestURI(), "/api/")) {
            chain.doFilter(request, response);
            return;
        }
        if (needClose(req)) {
            res.setStatus(CODE_416);
            return;
        }

        EmbyContentCacheReqWrapper reqWrapper = new EmbyContentCacheReqWrapper(req);
        try {
            if (originReqService.notGetReq(res, reqWrapper)) {
                return;
            }
            EmbyPicType picType = getPicType(req);
            if (非图片 != picType) {
                picRedirectService.processPic(reqWrapper, res, picType);
            } else {
                String mediaSourceId = ReUtil.isVideoUrl(req.getRequestURI());
                if (StrUtil.isNotBlank(mediaSourceId)) {
                    videoRedirectService.processVideo(reqWrapper, res);
                } else {
                    mediaSourceId = ReUtil.isAudioUrl(req.getRequestURI());
                    if (StrUtil.isNotBlank(mediaSourceId)) {
                        reqWrapper.setMediaSourceId(mediaSourceId);
                        videoRedirectService.processVideo(reqWrapper, res);
                    } else if (StrUtil.endWithIgnoreCase(reqWrapper.getRequestURI(), "/Views")) {
                        virtualService.handleViews(reqWrapper, res);
                    } else if (StrUtil.endWithAnyIgnoreCase(reqWrapper.getRequestURI(), "/Shows/NextUp")) {
                        virtualService.handleShowNext(reqWrapper, res);
                    } else if (StrUtil.endWithAnyIgnoreCase(reqWrapper.getRequestURI(), "/Items/Resume")) {
                        virtualService.handleUserResume(reqWrapper, res);
                    } else if (StrUtil.endWithIgnoreCase(reqWrapper.getRequestURI(), "/Items/Latest")) {
                        virtualService.handleLatest(reqWrapper, res);
                    } else if (isMimickedId(reqWrapper.getParentId())) {
                        virtualService.handle(reqWrapper, res);
                    } else {
                        originReqService.forwardOriReq(reqWrapper, res);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("转发请求失败[{}]: {}?{},e:", req.getMethod(),
                    reqWrapper.getRequestURI(), LogSanitizer.sanitizeQueryString(reqWrapper.getQueryString()), e);
            res.setStatus(CODE_504);
            return;
        } finally {
            accessLog.log(reqWrapper.getMethod(), reqWrapper.getRequestURI(), reqWrapper.getIp(),
                    req.getQueryString(), reqWrapper.getCachedHeader(),
                    res.getStatus(), start);
        }
    }
}
