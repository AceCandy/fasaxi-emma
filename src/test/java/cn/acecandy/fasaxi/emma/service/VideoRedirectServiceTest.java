package cn.acecandy.fasaxi.emma.service;

import cn.acecandy.fasaxi.emma.config.EmbyConfig;
import cn.acecandy.fasaxi.emma.config.EmbyContentCacheReqWrapper;
import cn.acecandy.fasaxi.emma.common.enums.CloudStorageType;
import cn.acecandy.fasaxi.emma.dao.embyboss.entity.VideoPathRelation;
import cn.acecandy.fasaxi.emma.dao.embyboss.service.VideoPathRelationDao;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.client.RedisLockClient;
import cn.acecandy.fasaxi.emma.sao.out.EmbyItem;
import cn.acecandy.fasaxi.emma.sao.out.EmbyMediaSource;
import cn.acecandy.fasaxi.emma.sao.proxy.EmbyProxy;
import cn.acecandy.fasaxi.emma.utils.CloudUtil;
import cn.acecandy.fasaxi.emma.utils.ThreadLimitUtil;
import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.core.lang.mutable.MutablePair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static jakarta.servlet.http.HttpServletResponse.SC_FOUND;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoRedirectServiceTest {

    @Mock
    private RedisClient redisClient;
    @Mock
    private RedisLockClient redisLockClient;
    @Mock
    private EmbyProxy embyProxy;
    @Mock
    private VideoPathRelationDao videoPathRelationDao;
    @Mock
    private ThreadLimitUtil threadLimitUtil;
    @Mock
    private CloudUtil cloudUtil;

    @Test
    void processVideo_localRelationWithoutConfiguredMapping_shouldFallbackInsteadOfThrowingNpe() throws Exception {
        VideoRedirectService service = buildService(emptyConfig());

        String mediaSourceId = "5553064";
        String realPath = "/mnt/all/cd2/new115/emby-jm/电视剧/综艺/2010/爱情保卫战 (2010) [tmdbid=119171]/Season 17/爱情保卫战.S17E01.2160p.WEB-DL.SDR.HEVC.mkv";

        MockHttpServletRequest rawRequest = new MockHttpServletRequest("GET", "/emby/videos/5553064/original.mkv");
        rawRequest.addParameter("MediaSourceId", "mediasource_" + mediaSourceId);
        rawRequest.addParameter("DeviceId", "device-1");
        EmbyContentCacheReqWrapper request = new EmbyContentCacheReqWrapper(rawRequest);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(redisClient.getStrFindOne(anyList())).thenReturn(null);
        when(redisLockClient.lock(eq("lock:video:" + mediaSourceId))).thenReturn(true);
        when(videoPathRelationDao.findById(5553064)).thenReturn(VideoPathRelation.x()
                .setItemId(5553064)
                .setStrmType("local")
                .setRealPath(realPath));
        when(embyProxy.getItemInfoByCache(mediaSourceId)).thenReturn(ListUtil.of(buildMovieItem(realPath)));

        assertDoesNotThrow(() -> service.processVideo(request, response));
        assertEquals(SC_FOUND, response.getStatus());
        assertEquals(realPath, response.getHeader("Location"));
        verify(redisLockClient).unlock("lock:video:" + mediaSourceId);
    }

    @Test
    void processVideo_localRelationWithStrmAlias_shouldNormalizeTo115Redirect() throws Exception {
        EmbyConfig embyConfig = emptyConfig();
        embyConfig.setStrmPaths(new String[]{
                "/mnt/all/cd2/new115/::http://192.168.1.249:5244/d/new115/"
        });
        VideoRedirectService service = buildService(embyConfig);

        String mediaSourceId = "5553064";
        String realPath = "/mnt/all/cd2/new115/emby-jm/电视剧/综艺/2010/爱情保卫战 (2010) [tmdbid=119171]/Season 17/爱情保卫战.S17E01.2160p.WEB-DL.SDR.HEVC.mkv";
        String normalizedPath = "http://192.168.1.249:5244/d/new115/emby-jm/电视剧/综艺/2010/爱情保卫战 (2010) [tmdbid=119171]/Season 17/爱情保卫战.S17E01.2160p.WEB-DL.SDR.HEVC.mkv";
        String redirect302 = "https://cdn.example.com/video.mkv?t=1919810";

        EmbyContentCacheReqWrapper request = buildRequest(mediaSourceId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(redisClient.getStrFindOne(anyList())).thenReturn(null);
        when(redisLockClient.lock(eq("lock:video:" + mediaSourceId))).thenReturn(true);
        when(videoPathRelationDao.findById(5553064)).thenReturn(VideoPathRelation.x()
                .setItemId(5553064)
                .setStrmType("local")
                .setRealPath(realPath));
        when(embyProxy.getItemInfoByCache(mediaSourceId)).thenReturn(ListUtil.of(buildMovieItem(realPath)));
        when(threadLimitUtil.limitThreadCache(eq(normalizedPath)))
                .thenReturn(MutablePair.of(CloudStorageType.R_115,
                        "/emby-jm/电视剧/综艺/2010/爱情保卫战 (2010) [tmdbid=119171]/Season 17/爱情保卫战.S17E01.2160p.WEB-DL.SDR.HEVC.mkv"));
        when(cloudUtil.getDownloadUrlOnCopyByOpenlist(eq(CloudStorageType.R_115), eq(request.getUa()),
                eq(request.getDeviceId()), eq("/emby-jm/电视剧/综艺/2010/爱情保卫战 (2010) [tmdbid=119171]/Season 17/爱情保卫战.S17E01.2160p.WEB-DL.SDR.HEVC.mkv")))
                .thenReturn(redirect302);

        service.processVideo(request, response);

        assertEquals(SC_FOUND, response.getStatus());
        assertEquals(redirect302, response.getHeader("Location"));
        verify(threadLimitUtil).limitThreadCache(normalizedPath);
        verify(redisLockClient).unlock("lock:video:" + mediaSourceId);
    }

    @Test
    void processVideo_withoutRelation_shouldNormalizeStrmAliasBeforeDispatch() throws Exception {
        EmbyConfig embyConfig = emptyConfig();
        embyConfig.setStrmPaths(new String[]{
                "/mnt/all/cd2/new115/::http://192.168.1.249:5244/d/new115/"
        });
        VideoRedirectService service = buildService(embyConfig);

        String mediaSourceId = "5553064";
        String realPath = "/mnt/all/cd2/new115/emby-jm/电视剧/综艺/2010/爱情保卫战 (2010) [tmdbid=119171]/Season 17/爱情保卫战.S17E01.2160p.WEB-DL.SDR.HEVC.mkv";
        String normalizedPath = "http://192.168.1.249:5244/d/new115/emby-jm/电视剧/综艺/2010/爱情保卫战 (2010) [tmdbid=119171]/Season 17/爱情保卫战.S17E01.2160p.WEB-DL.SDR.HEVC.mkv";
        String redirect302 = "https://cdn.example.com/video2.mkv?t=1919810";

        EmbyContentCacheReqWrapper request = buildRequest(mediaSourceId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(redisClient.getStrFindOne(anyList())).thenReturn(null);
        when(redisLockClient.lock(eq("lock:video:" + mediaSourceId))).thenReturn(true);
        when(videoPathRelationDao.findById(5553064)).thenReturn(null);
        when(embyProxy.getItemInfoByCache(mediaSourceId)).thenReturn(ListUtil.of(buildMovieItem(realPath)));
        when(threadLimitUtil.limitThreadCache(eq(normalizedPath)))
                .thenReturn(MutablePair.of(CloudStorageType.R_115,
                        "/emby-jm/电视剧/综艺/2010/爱情保卫战 (2010) [tmdbid=119171]/Season 17/爱情保卫战.S17E01.2160p.WEB-DL.SDR.HEVC.mkv"));
        when(cloudUtil.getDownloadUrlOnCopyByOpenlist(eq(CloudStorageType.R_115), eq(request.getUa()),
                eq(request.getDeviceId()), eq("/emby-jm/电视剧/综艺/2010/爱情保卫战 (2010) [tmdbid=119171]/Season 17/爱情保卫战.S17E01.2160p.WEB-DL.SDR.HEVC.mkv")))
                .thenReturn(redirect302);

        service.processVideo(request, response);

        assertEquals(SC_FOUND, response.getStatus());
        assertEquals(redirect302, response.getHeader("Location"));
        verify(threadLimitUtil).limitThreadCache(normalizedPath);
        verify(cloudUtil).cacheOpenList302Url(org.mockito.ArgumentMatchers.any(), eq(mediaSourceId), eq(request.getDeviceId()));
        verify(redisLockClient).unlock("lock:video:" + mediaSourceId);
    }

    private VideoRedirectService buildService(EmbyConfig embyConfig) {
        VideoRedirectService service = new VideoRedirectService();
        ReflectionTestUtils.setField(service, "redisClient", redisClient);
        ReflectionTestUtils.setField(service, "redisLockClient", redisLockClient);
        ReflectionTestUtils.setField(service, "embyProxy", embyProxy);
        ReflectionTestUtils.setField(service, "videoPathRelationDao", videoPathRelationDao);
        ReflectionTestUtils.setField(service, "embyConfig", embyConfig);
        ReflectionTestUtils.setField(service, "threadLimitUtil", threadLimitUtil);
        ReflectionTestUtils.setField(service, "cloudUtil", cloudUtil);
        return service;
    }

    private EmbyConfig emptyConfig() {
        EmbyConfig embyConfig = new EmbyConfig();
        embyConfig.setLocalPaths(ListUtil.empty());
        embyConfig.setOriginPt("http://origin.example.com");
        return embyConfig;
    }

    private EmbyContentCacheReqWrapper buildRequest(String mediaSourceId) throws Exception {
        MockHttpServletRequest rawRequest = new MockHttpServletRequest("GET", "/emby/videos/5553064/original.mkv");
        rawRequest.addParameter("MediaSourceId", "mediasource_" + mediaSourceId);
        rawRequest.addParameter("DeviceId", "device-1");
        return new EmbyContentCacheReqWrapper(rawRequest);
    }

    private EmbyItem buildMovieItem(String realPath) {
        EmbyMediaSource mediaSource = new EmbyMediaSource();
        mediaSource.setPath(realPath);

        EmbyItem item = new EmbyItem();
        item.setItemId("5553064");
        item.setType("Movie");
        item.setMediaSources(ListUtil.of(mediaSource));
        item.setSize(1L);
        return item;
    }
}
