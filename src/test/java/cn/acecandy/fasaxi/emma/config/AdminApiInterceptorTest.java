package cn.acecandy.fasaxi.emma.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminApiInterceptorTest {

    @Test
    void preHandle_disabledAdminApi_shouldRejectRequest() throws Exception {
        AdminApiProperties properties = buildProperties(false, "secret", List.of("local"));
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");
        AdminApiInterceptor interceptor = new AdminApiInterceptor(properties, environment);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/build/tmdb-douban");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertTrue(response.getContentAsString().contains("管理接口未启用"));
        assertTrue(response.getStatus() >= 400);
    }

    @Test
    void preHandle_missingToken_shouldRejectRequest() throws Exception {
        AdminApiProperties properties = buildProperties(true, "secret", List.of("local"));
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");
        AdminApiInterceptor interceptor = new AdminApiInterceptor(properties, environment);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/build/tmdb-douban");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertTrue(response.getContentAsString().contains("管理接口鉴权失败"));
        assertTrue(response.getStatus() >= 400);
    }

    @Test
    void preHandle_validTokenAndProfile_shouldAllowRequest() throws Exception {
        AdminApiProperties properties = buildProperties(true, "secret", List.of("local"));
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");
        AdminApiInterceptor interceptor = new AdminApiInterceptor(properties, environment);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/build/tmdb-douban");
        request.addHeader("X-Admin-Token", "secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    private AdminApiProperties buildProperties(boolean enabled, String token, List<String> allowedProfiles) {
        AdminApiProperties properties = new AdminApiProperties();
        properties.setEnabled(enabled);
        properties.setToken(token);
        properties.setAllowedProfiles(allowedProfiles);
        return properties;
    }
}
