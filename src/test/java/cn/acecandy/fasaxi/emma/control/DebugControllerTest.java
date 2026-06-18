package cn.acecandy.fasaxi.emma.control;

import cn.acecandy.fasaxi.emma.common.resp.Rsres;
import cn.acecandy.fasaxi.emma.sao.dto.Rile;
import cn.acecandy.fasaxi.emma.utils.CloudUtil;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
class DebugControllerTest {

    @Mock
    private CloudUtil cloudUtil;

    @Test
    void getFile_invalidCloud_shouldReturnBadRequest() {
        DebugController controller = new DebugController();
        ReflectionTestUtils.setField(controller, "cloudUtil", cloudUtil);

        ResponseEntity<Rsres<Object>> response = controller.getFile("unknown", "/movie/demo.mkv");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getMsg().contains("不支持的 cloud 参数"));
        assertNull(response.getBody().getResult());
        verify(cloudUtil, never()).getFile(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void getFile_validCloud_shouldCallCloudUtil() {
        DebugController controller = new DebugController();
        ReflectionTestUtils.setField(controller, "cloudUtil", cloudUtil);
        Rile rile = Rile.builder().fileId(1L).fileName("demo.mkv").build();
        when(cloudUtil.getFile(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("/movie/demo.mkv")))
                .thenReturn(rile);

        ResponseEntity<Rsres<Object>> response = controller.getFile("115", "/movie/demo.mkv");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(rile, response.getBody().getResult());
        verify(cloudUtil).getFile(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("/movie/demo.mkv"));
    }
}
