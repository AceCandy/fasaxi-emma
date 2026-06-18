package cn.acecandy.fasaxi.emma.config;

import cn.acecandy.fasaxi.emma.common.resp.Rsres;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.json.JSONUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

/**
 * 管理接口访问拦截器
 *
 * @author Codex
 * @since 2026/06/10
 */
@Component
@RequiredArgsConstructor
public class AdminApiInterceptor implements HandlerInterceptor {

    private final AdminApiProperties adminApiProperties;
    private final Environment environment;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!adminApiProperties.isEnabled()) {
            writeError(response, HttpStatus.FORBIDDEN, "管理接口未启用");
            return false;
        }
        if (!isProfileAllowed()) {
            writeError(response, HttpStatus.FORBIDDEN, "当前环境不允许访问管理接口");
            return false;
        }

        String configuredToken = adminApiProperties.getToken();
        if (StrUtil.isBlank(configuredToken)) {
            writeError(response, HttpStatus.FORBIDDEN, "管理接口令牌未配置");
            return false;
        }

        String requestToken = resolveRequestToken(request);
        if (StrUtil.isBlank(requestToken) || !equalsToken(configuredToken, requestToken)) {
            writeError(response, HttpStatus.UNAUTHORIZED, "管理接口鉴权失败");
            return false;
        }
        return true;
    }

    private boolean isProfileAllowed() {
        List<String> allowedProfiles = adminApiProperties.getAllowedProfiles();
        if (CollUtil.isEmpty(allowedProfiles)) {
            return true;
        }
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        return allowedProfiles.stream().filter(StrUtil::isNotBlank).anyMatch(activeProfiles::contains);
    }

    private String resolveRequestToken(HttpServletRequest request) {
        String token = request.getHeader(adminApiProperties.getHeaderName());
        if (StrUtil.isNotBlank(token)) {
            return token;
        }
        String authorization = request.getHeader("Authorization");
        if (StrUtil.startWithIgnoreCase(authorization, "Bearer ")) {
            return StrUtil.trim(StrUtil.removePrefixIgnoreCase(authorization, "Bearer "));
        }
        return null;
    }

    private boolean equalsToken(String expected, String actual) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws Exception {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSONUtil.toJsonStr(Rsres.fail(message)));
    }
}
