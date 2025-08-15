package cn.acecandy.fasaxi.emma.config;

import cn.acecandy.fasaxi.emma.utils.ReUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.Getter;
import lombok.Setter;
import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.collection.ListUtil;
import org.dromara.hutool.core.map.MapUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.core.text.split.SplitUtil;
import org.dromara.hutool.http.HttpUtil;
import org.dromara.hutool.http.server.servlet.ServletUtil;
import org.dromara.hutool.json.JSONArray;
import org.dromara.hutool.json.JSONObject;
import org.dromara.hutool.json.JSONUtil;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.dromara.hutool.core.text.StrPool.COMMA;

/**
 * emby请求体缓存包装器
 *
 * @author tangningzhu
 * @since 2025/4/15
 */
public class EmbyContentCacheReqWrapper extends HttpServletRequestWrapper {


    private final Map<String, String> paramMap;

    @Getter
    private final String cachedBody;
    @Getter
    private final Map<String, String> cachedHeader = MapUtil.newHashMap();
    @Getter
    private String ua;
    @Getter
    private String range;
    @Getter
    private String paramUri;
    @Getter
    @Setter
    public String mediaSourceId;
    @Getter
    public String parentId;
    @Getter
    private String userId;
    @Getter
    private String deviceId;
    @Getter
    private String apikey;
    @Getter
    private String ip;
    @Getter
    private final Map<String, Object> cachedParam = new TreeMap<>();

    @Getter
    private JSONArray toolkitView;

    public void buildToolKit(String toolkitView) {
        if (!JSONUtil.isTypeJSON(toolkitView)) {
            return;
        }
        JSONObject viewJn = JSONUtil.parseObj(toolkitView);
        this.toolkitView = viewJn.getJSONArray("Items");
    }

    public EmbyContentCacheReqWrapper(HttpServletRequest request) throws IOException {
        super(request);
        this.paramMap = ServletUtil.getParamMap(request);

        this.cachedBody = ServletUtil.getBody(request);
        parseFormData(this.cachedBody);
        cacheHeader(request);
        cacheParam(request);
    }

    private void parseFormData(String formData) {
        if (StrUtil.isBlank(formData)) {
            return;
        }
        if (JSONUtil.isTypeJSON(formData)) {
            JSONObject jsonData = JSONUtil.parseObj(formData);
            if (jsonData.containsKey("ItemId")) {
                mediaSourceId = jsonData.getStr("ItemId");
            }
            if (jsonData.containsKey("ParentId")) {
                parentId = jsonData.getStr("ParentId");
            }
            return;
        }
        for (String pair : SplitUtil.splitTrim(formData, "&")) {
            List<String> entrys = SplitUtil.splitTrim(pair, "=");
            if (CollUtil.size(entrys) < 2) {
                continue;
            }
            String key = CollUtil.getFirst(entrys);
            String value = CollUtil.getLast(entrys);
            if (StrUtil.equalsIgnoreCase(key, "ItemId")) {
                mediaSourceId = key;
            }
            if (StrUtil.equalsIgnoreCase(key, "ParentId")) {
                parentId = key;
            }
            paramMap.computeIfAbsent(key, v -> value);
        }
    }

    private void cacheHeader(HttpServletRequest request) throws IOException {
        Map<String, String> headerMap = MapUtil.newHashMap();
        List<String> headerNameList = ListUtil.of(request.getHeaderNames());
        headerNameList.forEach(headerName -> {
            if (StrUtil.equalsAnyIgnoreCase(headerName, "Connection", "Keep-Alive", "Proxy-Connection",
                    "Transfer-Encoding", "Host", "Upgrade", "TE", "Trailer")) {
                return;
            }
            // if (StrUtil.equalsAnyIgnoreCase(headerName, "Host", "Referer")) {
            //     return;
            // }
            String headerValue = request.getHeader(headerName);
            if (StrUtil.equalsIgnoreCase(headerName, "User-Agent")) {
                this.ua = headerValue;
            }
            /*if (StrUtil.equalsIgnoreCase(headerName, "Accept-Encoding")) {
                headerValue = "gzip";
            }*/
            if (StrUtil.equalsIgnoreCase(headerName, "Range")) {
                this.range = headerValue;
            }
            if (StrUtil.equalsIgnoreCase(headerName, "X-Emby-Token")) {
                this.apikey = headerValue;
            }
            if (StrUtil.equalsAnyIgnoreCase(headerName, "X-Emby-Authorization", "Authorization")) {
                for (String auth : SplitUtil.splitTrim(headerValue, ",")) {
                    List<String> authParts = SplitUtil.splitTrim(auth, "=");
                    if (CollUtil.size(authParts) < 2) {
                        continue;
                    }
                    String key = CollUtil.getFirst(authParts);
                    String value = StrUtil.strip(CollUtil.getLast(authParts), "\"");
                    if (StrUtil.equalsIgnoreCase(key, "Emby UserId")) {
                        this.userId = value;
                    } else if (StrUtil.equalsIgnoreCase(key, "DeviceId")) {
                        this.deviceId = value;
                    } else if (StrUtil.equalsAnyIgnoreCase(key, "Token")) {
                        this.apikey = value;
                    }
                }
            }
            headerMap.put(ReUtil.capitalizeWords(headerName), headerValue);
        });
        String xffHeader = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(xffHeader)) {
            ip = CollUtil.getFirst(SplitUtil.splitTrim(xffHeader, COMMA));
        } else {
            ip = request.getRemoteAddr();
        }
        cachedHeader.putAll(headerMap);
    }

    private void cacheParam(HttpServletRequest request) {
        String uri = request.getRequestURI().toLowerCase();
        if (StrUtil.containsAnyIgnoreCase(uri, "/images/primary")) {
            cachedParam.put("tag", request.getParameter("tag"));
            cachedParam.put("maxWidth", "342");
            // cachedParam.put("quality", "90");
        } else if (StrUtil.containsAnyIgnoreCase(uri, "/images/logo")) {
            cachedParam.put("tag", request.getParameter("tag"));
            cachedParam.put("maxWidth", "154");
        } else if (StrUtil.containsAnyIgnoreCase(uri, "/images/backdrop")) {
            cachedParam.put("tag", request.getParameter("tag"));
            cachedParam.put("maxWidth", "780");
        } else {
            List<String> itemUrlGroup = ReUtil.isSimilarItemUrl(uri);
            if (CollUtil.isNotEmpty(itemUrlGroup)) {
                userId = CollUtil.getFirst(itemUrlGroup);
                mediaSourceId = CollUtil.getLast(itemUrlGroup);
            }
            String itemId = ReUtil.isPlaylistUrl(request.getRequestURI());
            if (StrUtil.isBlank(mediaSourceId) && StrUtil.isNotBlank(itemId)) {
                mediaSourceId = itemId;
            }
            if (MapUtil.isNotEmpty(paramMap)) {
                cachedParam.putAll(paramMap);
                // if (cachedParam.containsKey("searchterm")) {
                if (cachedParam.containsKey("IncludeItemTypes")) {
                    cachedParam.put("IncludeItemTypes", StrUtil.replaceIgnoreCase(
                            cachedParam.get("IncludeItemTypes").toString(), ",BoxSet", ""));
                }
                if (cachedParam.containsKey("MediaSourceId")) {
                    mediaSourceId = StrUtil.removePrefixIgnoreCase(
                            cachedParam.get("MediaSourceId").toString(), "mediasource_");
                }
                if (cachedParam.containsKey("UserId")) {
                    userId = MapUtil.getStr(cachedParam, "UserId");
                }
                if (cachedParam.containsKey("X-Emby-Device-Id")) {
                    if (StrUtil.isBlank(deviceId)) {
                        deviceId = MapUtil.getStr(cachedParam, "X-Emby-Device-Id");
                    }
                }
                if (cachedParam.containsKey("DeviceId")) {
                    if (StrUtil.isBlank(deviceId)) {
                        deviceId = MapUtil.getStr(cachedParam, "DeviceId");
                    }
                }
                if (cachedParam.containsKey("X-Emby-Token")) {
                    apikey = MapUtil.getStr(cachedParam, "X-Emby-Token");
                }
                if (cachedParam.containsKey("ParentId")) {
                    parentId = MapUtil.getStr(cachedParam, "ParentId");
                }
            }
        }
        paramUri = HttpUtil.urlWithFormUrlEncoded(request.getRequestURI(), cachedParam, Charset.defaultCharset());
    }

}