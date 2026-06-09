package cn.acecandy.fasaxi.emma.service;

import cn.acecandy.fasaxi.emma.common.resp.EmbyCachedResp;
import cn.acecandy.fasaxi.emma.config.EmbyConfig;
import cn.acecandy.fasaxi.emma.config.EmbyContentCacheReqWrapper;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.hutool.v7.http.client.Request;
import cn.hutool.v7.http.client.Response;
import cn.hutool.v7.http.client.engine.ClientEngine;
import cn.hutool.v7.http.meta.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OriginReqServiceTest {

    @Mock
    private RedisClient redisClient;
    @Mock
    private ClientEngine httpClient;

    private OriginReqService service;

    @BeforeEach
    void setUp() {
        service = buildService(8);
    }

    @Test
    void notGetReq_nonGetJsonResponse_shouldForwardJsonResponseAndClearCache() throws Exception {
        Response originResponse = org.mockito.Mockito.mock(Response.class);
        when(originResponse.getStatus()).thenReturn(201);
        when(originResponse.header("Content-Type")).thenReturn("application/json; charset=utf-8");
        when(originResponse.header("Content-Encoding")).thenReturn(null);
        when(originResponse.headers()).thenReturn(Map.of(
                "Content-Type", List.of("application/json; charset=utf-8"),
                "X-Origin", List.of("ok")
        ));
        when(originResponse.bodyBytes()).thenReturn("{\"ok\":true}".getBytes(StandardCharsets.UTF_8));
        when(httpClient.send(any(Request.class))).thenReturn(originResponse);

        MockHttpServletRequest rawRequest = new MockHttpServletRequest("POST", "/emby/Users/user-1/Items");
        rawRequest.addHeader("X-Emby-Token", "token-1");
        rawRequest.addParameter("UserId", "user-1");
        rawRequest.setContent("{\"Name\":\"demo\"}".getBytes(StandardCharsets.UTF_8));
        rawRequest.setContentType("application/json");
        EmbyContentCacheReqWrapper request = new EmbyContentCacheReqWrapper(rawRequest);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(service.notGetReq(response, request));

        assertEquals(201, response.getStatus());
        assertEquals("ok", response.getHeader("X-Origin"));
        assertEquals("{\"ok\":true}", response.getContentAsString());

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(httpClient).send(requestCaptor.capture());
        Request forwardedRequest = requestCaptor.getValue();
        assertEquals(Method.POST, forwardedRequest.method());
        assertTrue(forwardedRequest.url().toString().contains("/emby/Users/user-1/Items"));
        assertTrue(forwardedRequest.url().toString().contains("api_key=token-1"));
        assertEquals("{\"Name\":\"demo\"}", forwardedRequest.bodyStr());

        verify(redisClient).delByPrefix(org.mockito.ArgumentMatchers.<List<String>>argThat(keys ->
                keys.contains("cache:req:/emby/Users/user-1/Items")
                        && keys.contains("cache:req:/Users/user-1/Items")));
    }

    @Test
    void isOriginResponseCacheable_getOkAndSmallBody_shouldCache() throws Exception {
        EmbyContentCacheReqWrapper request = buildRequest("GET");
        EmbyCachedResp cached = buildCachedResp(200, 8);

        assertTrue(service.isOriginResponseCacheable(request, cached));
    }

    @Test
    void isOriginResponseCacheable_largeBody_shouldSkipCache() throws Exception {
        EmbyContentCacheReqWrapper request = buildRequest("GET");
        EmbyCachedResp cached = buildCachedResp(200, 9);

        assertFalse(service.isOriginResponseCacheable(request, cached));
    }

    @Test
    void isOriginResponseCacheable_nonGet_shouldSkipCache() throws Exception {
        EmbyContentCacheReqWrapper request = buildRequest("POST");
        EmbyCachedResp cached = buildCachedResp(200, 4);

        assertFalse(service.isOriginResponseCacheable(request, cached));
    }

    @Test
    void isOriginResponseCacheable_nonOkStatus_shouldSkipCache() throws Exception {
        EmbyContentCacheReqWrapper request = buildRequest("GET");
        EmbyCachedResp cached = buildCachedResp(204, 0);

        assertFalse(service.isOriginResponseCacheable(request, cached));
    }

    private OriginReqService buildService(int maxBodyBytes) {
        OriginReqService service = new OriginReqService();
        EmbyConfig embyConfig = new EmbyConfig();
        embyConfig.setHost("http://emby.example.com");
        embyConfig.setOriginCacheMaxBodyBytes(maxBodyBytes);
        ReflectionTestUtils.setField(service, "embyConfig", embyConfig);
        ReflectionTestUtils.setField(service, "redisClient", redisClient);
        ReflectionTestUtils.setField(service, "httpClient", httpClient);
        return service;
    }

    private EmbyContentCacheReqWrapper buildRequest(String method) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/emby/Items");
        request.addParameter("UserId", "user-1");
        return new EmbyContentCacheReqWrapper(request);
    }

    private EmbyCachedResp buildCachedResp(int status, int bodySize) {
        EmbyCachedResp cached = new EmbyCachedResp();
        cached.setStatusCode(status);
        cached.setContent(new byte[bodySize]);
        return cached;
    }
}
