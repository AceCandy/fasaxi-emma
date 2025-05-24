package cn.acecandy.fasaxi.emma.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import org.springframework.core.annotation.Order;

import java.io.IOException;

/**
 * api路径直接放行
 *
 * @author AceCandy
 * @since 2025/05/25
 */
@WebFilter(urlPatterns = "/api/*")
@Order(1) // 优先级最高
public class ApiBypassFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        chain.doFilter(req, res);
    }
}