package cn.acecandy.fasaxi.emma.config;

import cn.acecandy.fasaxi.emma.common.enums.EmbyPicType;
import cn.acecandy.fasaxi.emma.service.OriginReqService;
import cn.acecandy.fasaxi.emma.service.PicRedirectService;
import cn.acecandy.fasaxi.emma.service.VideoRedirectService;
import cn.acecandy.fasaxi.emma.service.VirtualService;
import cn.acecandy.fasaxi.emma.utils.FileCacheUtil;
import cn.acecandy.fasaxi.emma.utils.ReUtil;
import cn.hutool.v7.core.date.DateUtil;
import cn.hutool.v7.core.text.StrUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_416;
import static cn.acecandy.fasaxi.emma.common.constants.CacheConstant.CODE_504;
import static cn.acecandy.fasaxi.emma.common.enums.EmbyPicType.非图片;
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
    private EmbyConfig embyConfig;

    @Resource
    private FileCacheUtil fileCacheUtil;

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

        /*if (isWebSocketHandshake(req)) {
            log.warn("WebSocket请求: {}", req.getRequestURI());
            handleWebSocket(req, res);
            return;
        }*/
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
                    } else if (StrUtil.startWith(reqWrapper.getParentId(), "-")) {
                        virtualService.handle(reqWrapper, res);
                    } else {
                        originReqService.forwardOriReq(reqWrapper, res);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("转发请求失败[{}]: {}?{},e:", req.getMethod(),
                    reqWrapper.getRequestURI(), reqWrapper.getQueryString(), e);
            res.setStatus(CODE_504);
            return;
            /*if (ServletUtil.isGetMethod(req)) {
                res.setStatus(HttpServletResponse.SC_FOUND);
                String url302 = embyConfig.getOuterHost() + reqWrapper.getParamUri();
                res.setHeader("Location", url302);
                log.info("302原始转发->[{}]", url302);
            }*/
            // originReqService.forwardOriReq(reqWrapper, res);
        } finally {
            accessLog.log(reqWrapper.getMethod(), reqWrapper.getRequestURI(), reqWrapper.getIp(),
                    req.getQueryString(), reqWrapper.getCachedHeader(), reqWrapper.getApiKey(),
                    res.getStatus(), start);
        }
    }

    private boolean isWebSocketHandshake(HttpServletRequest request) {
        String connection = request.getHeader("Connection");
        String upgrade = request.getHeader("Upgrade");
        return "Upgrade".equalsIgnoreCase(connection) && "websocket".equalsIgnoreCase(upgrade);
    }

    // WebSocket请求转发实现
    @SneakyThrows
    private void handleWebSocket(HttpServletRequest req, HttpServletResponse res) {
        try {
            URI targetUri = new URI(embyConfig.getWsHost() + req.getRequestURI());
            HttpClient client = HttpClient.newHttpClient();
            AsyncContext asyncContext = req.startAsync();

            WebSocket targetSocket = client.newWebSocketBuilder()
                    // .header("Sec-WebSocket-Key", req.getHeader("Sec-WebSocket-Key"))
                    // .header("Sec-WebSocket-Version", req.getHeader("Sec-WebSocket-Version"))
                    // .header("Upgrade", "websocket")
                    // .header("Connection", "Upgrade")
                    .buildAsync(targetUri, new WebSocket.Listener() {
                        private StringBuilder textBuffer = new StringBuilder();

                        @Override
                        public void onOpen(WebSocket webSocket) {
                            webSocket.request(1);
                            log.info("WebSocket连接已建立: {}", targetUri);
                        }

                        @SneakyThrows
                        @Override
                        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                            textBuffer.append(data);
                            if (last) {
                                log.info("转发服务端消息: {}", textBuffer);
                                asyncContext.getResponse().getWriter().write(textBuffer.toString());
                                textBuffer.setLength(0);
                            }
                            webSocket.request(1);
                            return null;
                        }

                        @Override
                        public void onError(WebSocket webSocket, Throwable error) {
                            log.error("WebSocket错误: {}", error.getMessage());
                            webSocket.abort();
                            asyncContext.complete();
                        }
                    }).get(30, TimeUnit.SECONDS);

            ServletInputStream input = req.getInputStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            input.setReadListener(new ReadListener() {
                @Override
                public void onDataAvailable() throws IOException {
                    byte[] chunk = new byte[input.available()];
                    int read = input.read(chunk);
                    if (read > 0) {
                        buffer.write(chunk, 0, read);
                    }
                }

                @Override
                public void onAllDataRead() {
                    targetSocket.sendText(buffer.toString(StandardCharsets.UTF_8), true);
                    buffer.reset();
                }

                @Override
                public void onError(Throwable t) {
                    log.error("输入流异常: {}", t.getMessage());
                    targetSocket.abort();
                    asyncContext.complete();
                }
            });

        } catch (TimeoutException e) {
            log.error("连接超时: {}", e.getMessage());
            res.sendError(504, "WebSocket握手超时");
        } catch (Exception e) {
            log.error("内部错误: {}", e.getMessage());
            res.sendError(500, "服务器内部错误");
        }
    }

}