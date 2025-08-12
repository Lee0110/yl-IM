package com.lyl.filter;

import com.lyl.utils.MDCContextUtil;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * TraceId过滤器，用于在请求开始时生成traceId并放入MDC中，在请求结束时清除
 */
@Component
@Order(0)
public class TraceIdFilter extends OncePerRequestFilter {

    /**
     * TraceId的键名
     */
    public static final String TRACE_ID = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            // 尝试从请求头中获取traceId
            String traceId = request.getHeader(TRACE_ID);

            // 如果请求头中没有traceId，则生成一个新的
            if (!StringUtils.hasText(traceId)) {
                traceId = MDCContextUtil.generateTraceId();
            }

            // 将traceId放入MDC
            MDC.put(TRACE_ID, traceId);

            // 将traceId添加到响应头中
            response.addHeader(TRACE_ID, traceId);

            // 继续执行过滤链
            filterChain.doFilter(request, response);
        } finally {
            // 请求结束时清除MDC中的traceId
            MDC.remove(TRACE_ID);
        }
    }
}
