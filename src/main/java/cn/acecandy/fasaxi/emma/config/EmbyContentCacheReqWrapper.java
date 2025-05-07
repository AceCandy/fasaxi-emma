package cn.acecandy.fasaxi.emma.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.Getter;
import org.dromara.hutool.core.map.MapUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.http.HttpUtil;
import org.dromara.hutool.http.server.servlet.ServletUtil;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

/**
 * emby请求体缓存包装器
 *
 * @author tangningzhu
 * @since 2025/4/15
 */
public class EmbyContentCacheReqWrapper extends HttpServletRequestWrapper {
    @Getter
    private String cachedBody;
    @Getter
    private final Map<String, String> cachedHeader = MapUtil.newHashMap();
    @Getter
    private String ua;
    @Getter
    private String range;
    @Getter
    private String paramUri;
    @Getter
    private String mediaSourceId;
    @Getter
    private final Map<String, Object> cachedParam = new TreeMap<>();

    public EmbyContentCacheReqWrapper(HttpServletRequest request) throws IOException {
        super(request);
        cacheHeader(request);
        cacheParam(request);
        cacheInputStream(request);
    }

    private void cacheInputStream(HttpServletRequest request) throws IOException {
        this.cachedBody = ServletUtil.getBody(request);
    }

    private void cacheHeader(HttpServletRequest request) throws IOException {
        Map<String, String> headerMap = MapUtil.newHashMap();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (!StrUtil.equalsAnyIgnoreCase(headerName, "Host", "Content-Length", "Referer", "Transfer-Encoding")) {
                String headerValue = request.getHeader(headerName);
                headerMap.put(headerName, headerValue);
                if (StrUtil.equalsIgnoreCase(headerName, "User-Agent")) {
                    this.ua = headerValue;
                } else if (StrUtil.equalsIgnoreCase(headerName, "Range")) {
                    this.range = headerValue;
                }
            }
        }
        cachedHeader.putAll(headerMap);
    }

    private void cacheParam(HttpServletRequest request) {
        String uri = request.getRequestURI().toLowerCase();
        if (StrUtil.containsAnyIgnoreCase(uri, "/images/primary")) {
            cachedParam.put("tag", request.getParameter("tag"));
            cachedParam.put("maxwidth", "500");
            // cachedParam.put("quality", "90");
        } else if (StrUtil.containsAnyIgnoreCase(uri, "/images/logo")) {
            cachedParam.put("tag", request.getParameter("tag"));
            cachedParam.put("maxwidth", "400");
        } else if (StrUtil.containsAnyIgnoreCase(uri, "/images/backdrop")) {
            cachedParam.put("tag", request.getParameter("tag"));
            cachedParam.put("maxwidth", "1280");
        } else {
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            paramMap.forEach((k, v) -> cachedParam.put(k.toLowerCase(), v));
            // if (cachedParam.containsKey("searchterm")) {
            if (cachedParam.containsKey("includeitemtypes")) {
                cachedParam.put("includeitemtypes", StrUtil.replaceIgnoreCase(
                        cachedParam.get("includeitemtypes").toString(), ",BoxSet", ""));
            }
            if (cachedParam.containsKey("mediasourceid")) {
                mediaSourceId = StrUtil.removePrefixIgnoreCase(
                        cachedParam.get("mediasourceid").toString(), "mediasource_");
            }
        }
        paramUri = HttpUtil.urlWithFormUrlEncoded(request.getRequestURI(), cachedParam, Charset.defaultCharset());
    }

}