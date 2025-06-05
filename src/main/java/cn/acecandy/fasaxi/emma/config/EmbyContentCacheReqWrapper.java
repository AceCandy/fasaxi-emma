package cn.acecandy.fasaxi.emma.config;

import cn.acecandy.fasaxi.emma.utils.ReUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.Getter;
import lombok.Setter;
import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.collection.ListUtil;
import org.dromara.hutool.core.lang.Console;
import org.dromara.hutool.core.map.MapUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.core.text.split.SplitUtil;
import org.dromara.hutool.http.HttpUtil;
import org.dromara.hutool.http.server.servlet.ServletUtil;
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
    private String userId;
    @Getter
    private String deviceId;
    @Getter
    private String apikey;
    @Getter
    private String ip;
    @Getter
    private final Map<String, Object> cachedParam = new TreeMap<>();

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
            Console.log(jsonData);
            if (jsonData.containsKey("ItemId")) {
                mediaSourceId = jsonData.getStr("ItemId");
            }
            return;
        }
        for (String pair : SplitUtil.splitTrim(formData, "&")) {
            List<String> entrys = SplitUtil.splitTrim(pair, "=");
            if (CollUtil.size(entrys) < 2) {
                continue;
            }
            paramMap.computeIfAbsent(CollUtil.getFirst(entrys), v -> CollUtil.getLast(entrys));
        }
    }

    private void cacheHeader(HttpServletRequest request) throws IOException {
        Map<String, String> headerMap = MapUtil.newHashMap();
        List<String> headerNameList = ListUtil.of(request.getHeaderNames());
        headerNameList.forEach(headerName -> {
            if (StrUtil.equalsAnyIgnoreCase(headerName, "Host", "Content-Length",
                    "Referer", "Transfer-Encoding", "Content-Type")) {
                return;
            }
            String headerValue = request.getHeader(headerName);
            headerMap.put(headerName, headerValue);
            if (StrUtil.equalsIgnoreCase(headerName, "User-Agent")) {
                this.ua = headerValue;
            }
            if (StrUtil.equalsIgnoreCase(headerName, "Range")) {
                this.range = headerValue;
            }
            if (StrUtil.equalsIgnoreCase(headerName, "x-emby-token")) {
                this.apikey = headerValue;
            }
            if (StrUtil.equalsAnyIgnoreCase(headerName, "x-emby-authorization", "authorization")) {
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
            cachedParam.put("maxwidth", "342");
            // cachedParam.put("quality", "90");
        } else if (StrUtil.containsAnyIgnoreCase(uri, "/images/logo")) {
            cachedParam.put("tag", request.getParameter("tag"));
            cachedParam.put("maxwidth", "154");
        } else if (StrUtil.containsAnyIgnoreCase(uri, "/images/backdrop")) {
            cachedParam.put("tag", request.getParameter("tag"));
            cachedParam.put("maxwidth", "780");
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
                if (cachedParam.containsKey("userid")) {
                    userId = MapUtil.getStr(cachedParam, "userid");
                }
                if (cachedParam.containsKey("x-emby-device-id")) {
                    deviceId = MapUtil.getStr(cachedParam, "x-emby-device-id");
                }
                if (cachedParam.containsKey("x-emby-token")) {
                    apikey = MapUtil.getStr(cachedParam, "x-emby-token");
                }
            }
        }
        paramUri = HttpUtil.urlWithFormUrlEncoded(request.getRequestURI(), cachedParam, Charset.defaultCharset());
    }

    public static void main(String[] args) {
        String s = "MediaBrowser Client=\"Stream Music\", Device=\"MacdeMacBook Pro\", DeviceId=\"9B7SlF3MW6yaREEffJThdiZv\", Version=\"1.3.7\", Token=\"c2ba538641744dadb769c20df9a11ed1\"";

        for (String auth : SplitUtil.splitTrim(s, ",")) {
            List<String> authParts = SplitUtil.splitTrim(auth, "=");
            if (CollUtil.size(authParts) < 2) {
                continue;
            }
            String key = CollUtil.getFirst(authParts);
            // String value = StrUtil.strip(CollUtil.getLast(authParts), "\"");
            String value = CollUtil.getLast(authParts);
            if (StrUtil.equalsIgnoreCase(key, "Emby UserId")) {
                Console.log(value);
            } else if (StrUtil.equalsIgnoreCase(key, "DeviceId")) {
                Console.log(value);
            } else if (StrUtil.equalsAnyIgnoreCase(key, "Token")) {
                Console.log(value);
            }
        }
    }

}