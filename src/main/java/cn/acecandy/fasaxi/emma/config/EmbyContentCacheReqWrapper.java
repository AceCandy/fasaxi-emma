package cn.acecandy.fasaxi.emma.config;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    private byte[] cachedContent;
    private final Map<String, String> cachedHeader = MapUtil.newHashMap();
    private final Map<String, Object> cachedParam = new TreeMap<>();

    public EmbyContentCacheReqWrapper(HttpServletRequest request) throws IOException {
        super(request);
        cacheHeader(request);
        cacheParam(request);
        cacheInputStream(request);
    }

    private void cacheInputStream(HttpServletRequest request) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream is = request.getInputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) > -1) {
            baos.write(buffer, 0, len);
        }
        this.cachedContent = baos.toByteArray();
    }

    private void cacheHeader(HttpServletRequest request) throws IOException {
        Map<String, String> headerMap = MapUtil.newHashMap();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (!StrUtil.equalsAnyIgnoreCase(headerName, "Host", "Content-Length", "Referer")) {
                String headerValue = request.getHeader(headerName);
                /*if (StrUtil.equalsIgnoreCase(headerName, "User-Agent")) {
                    headerValue = "Yamby/1.0";
                }*/
                headerMap.put(headerName, headerValue);
            }
        }
        cachedHeader.putAll(headerMap);
    }

    private void cacheParam(HttpServletRequest request) {
        String uri = request.getRequestURI().toLowerCase();
        if (StrUtil.containsAnyIgnoreCase(uri, "/images/primary")) {
            cachedParam.put("tag", request.getParameter("tag"));
            cachedParam.put("maxWidth", "400");
            cachedParam.put("quality", "90");
        } else if (StrUtil.containsAnyIgnoreCase(uri, "/images/logo")) {
            cachedParam.put("tag", request.getParameter("tag"));
            cachedParam.put("maxWidth", "200");
            cachedParam.put("quality", "90");
        } else {
            Enumeration<String> paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                cachedParam.put(paramName, request.getParameter(paramName));
            }
        }
    }

    public byte[] getContentAsByteArray() {
        return this.cachedContent;
    }

    public Map<String, String> getHeaderMap() {
        return this.cachedHeader;
    }

    public Map<String, Object> getCacheParam() {
        return this.cachedParam;
    }
}