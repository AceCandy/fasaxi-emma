package cn.acecandy.fasaxi.emma.service;

import cn.acecandy.fasaxi.emma.common.resp.EmbyCachedResp;
import cn.acecandy.fasaxi.emma.config.EmbyConfig;
import cn.acecandy.fasaxi.emma.config.EmbyContentCacheReqWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OriginReqServiceTest {

    @Test
    void isOriginResponseCacheable_getOkAndSmallBody_shouldCache() throws Exception {
        OriginReqService service = buildService(8);
        EmbyContentCacheReqWrapper request = buildRequest("GET");
        EmbyCachedResp cached = buildCachedResp(200, 8);

        assertTrue(service.isOriginResponseCacheable(request, cached));
    }

    @Test
    void isOriginResponseCacheable_largeBody_shouldSkipCache() throws Exception {
        OriginReqService service = buildService(8);
        EmbyContentCacheReqWrapper request = buildRequest("GET");
        EmbyCachedResp cached = buildCachedResp(200, 9);

        assertFalse(service.isOriginResponseCacheable(request, cached));
    }

    @Test
    void isOriginResponseCacheable_nonGet_shouldSkipCache() throws Exception {
        OriginReqService service = buildService(8);
        EmbyContentCacheReqWrapper request = buildRequest("POST");
        EmbyCachedResp cached = buildCachedResp(200, 4);

        assertFalse(service.isOriginResponseCacheable(request, cached));
    }

    @Test
    void isOriginResponseCacheable_nonOkStatus_shouldSkipCache() throws Exception {
        OriginReqService service = buildService(8);
        EmbyContentCacheReqWrapper request = buildRequest("GET");
        EmbyCachedResp cached = buildCachedResp(204, 0);

        assertFalse(service.isOriginResponseCacheable(request, cached));
    }

    private OriginReqService buildService(int maxBodyBytes) {
        OriginReqService service = new OriginReqService();
        EmbyConfig embyConfig = new EmbyConfig();
        embyConfig.setOriginCacheMaxBodyBytes(maxBodyBytes);
        ReflectionTestUtils.setField(service, "embyConfig", embyConfig);
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
