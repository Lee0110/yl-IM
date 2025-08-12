package com.lyl.filter;

import com.lyl.interceptor.CachedBodyHttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 请求体缓存过滤器
 * 用于包装HttpServletRequest，使其请求体可以被多次读取
 * 确保该过滤器在处理链的最前面，优先级高于所有其他过滤器
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RequestBodyCacheFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 只处理可能包含请求体的请求方法
        String method = request.getMethod();
        if (("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method))
                && request.getContentType() != null
                && (request.getContentType().contains("application/json")
                    || request.getContentType().contains("application/xml")
                    || request.getContentType().contains("text/plain"))) {

            // 包装请求
            CachedBodyHttpServletRequest cachedBodyRequest = new CachedBodyHttpServletRequest(request);
            log.debug("已包装请求体，使其可以多次读取: {}", request.getRequestURI());

            // 继续过滤器链，使用包装后的请求
            filterChain.doFilter(cachedBodyRequest, response);
        } else {
            // 对于不需要包装的请求，直接继续
            filterChain.doFilter(request, response);
        }
    }
}
