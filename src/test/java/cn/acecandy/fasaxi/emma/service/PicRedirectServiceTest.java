package cn.acecandy.fasaxi.emma.service;

import cn.acecandy.fasaxi.emma.config.DoubanConfig;
import cn.acecandy.fasaxi.emma.config.EmbyContentCacheReqWrapper;
import cn.acecandy.fasaxi.emma.config.TmdbConfig;
import cn.acecandy.fasaxi.emma.dao.embyboss.service.EmbyItemPicDao;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.client.RedisLockClient;
import cn.acecandy.fasaxi.emma.sao.out.EmbyRemoteImageOut;
import cn.acecandy.fasaxi.emma.sao.proxy.EmbyProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static cn.acecandy.fasaxi.emma.common.enums.EmbyPicType.封面;
import static jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PicRedirectServiceTest {

    @Mock
    private OriginReqService originReqService;
    @Mock
    private EmbyProxy embyProxy;
    @Mock
    private EmbyItemPicDao embyItemPicDao;
    @Mock
    private RedisClient redisClient;
    @Mock
    private RedisLockClient redisLockClient;

    private PicRedirectService service;

    @BeforeEach
    void setUp() {
        service = new PicRedirectService();
        ReflectionTestUtils.setField(service, "originReqService", originReqService);
        ReflectionTestUtils.setField(service, "embyProxy", embyProxy);
        ReflectionTestUtils.setField(service, "embyItemPicDao", embyItemPicDao);
        ReflectionTestUtils.setField(service, "doubanConfig", doubanConfig());
        ReflectionTestUtils.setField(service, "tmdbConfig", tmdbConfig());
        ReflectionTestUtils.setField(service, "redisClient", redisClient);
        ReflectionTestUtils.setField(service, "redisLockClient", redisLockClient);
    }

    @Test
    void processPic_cacheHitUrl_shouldReturnCachedRedirectWithoutLock() throws Exception {
        EmbyContentCacheReqWrapper request = buildRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        String cachedUrl = "https://img.example.com/poster.jpg";
        when(redisClient.getStr("cache:pic:123|Primary")).thenReturn(cachedUrl);

        service.processPic(request, response, 封面);

        verify(originReqService).return308(response, cachedUrl);
        verify(redisLockClient, never()).lock("lock:pic:123|Primary");
    }

    @Test
    void processPic_remoteUndefined_shouldReturn404CacheUndefinedAndUnlock() throws Exception {
        EmbyContentCacheReqWrapper request = buildRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(redisClient.getStr("cache:pic:123|Primary")).thenReturn(null);
        when(embyItemPicDao.findByItemId(123)).thenReturn(null);
        when(redisLockClient.lock("lock:pic:123|Primary")).thenReturn(true);
        when(embyProxy.getRemoteImage("123", 封面))
                .thenReturn(EmbyRemoteImageOut.Img.builder().url("undefined").build());

        service.processPic(request, response, 封面);

        org.junit.jupiter.api.Assertions.assertEquals(SC_NOT_FOUND, response.getStatus());
        verify(redisClient).set("cache:pic:123|Primary", "undefined", 60 * 60);
        verify(redisLockClient).unlock("lock:pic:123|Primary");
    }

    private EmbyContentCacheReqWrapper buildRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/emby/Items/123/Images/Primary");
        return new EmbyContentCacheReqWrapper(request);
    }

    private DoubanConfig doubanConfig() {
        DoubanConfig config = new DoubanConfig();
        config.setImageStaticUrl("img.doubanio.com");
        return config;
    }

    private TmdbConfig tmdbConfig() {
        TmdbConfig config = new TmdbConfig();
        config.setImageStaticUrl("https://image.tmdb.org/t/p/original");
        return config;
    }
}
