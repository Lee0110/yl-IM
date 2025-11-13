package com.lyl.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 请求响应日志拦截器
 * 记录每个请求的URI、参数/请求体和响应内容
 */
@Slf4j
public class RequestResponseLogInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String timeAttribute = "requestStartTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod) {
            // 记录请求开始时间
            request.setAttribute(timeAttribute, System.currentTimeMillis());

            // 记录请求URI
            String uri = request.getRequestURI();
            String method = request.getMethod();
            log.info("请求开始 - URI: {}, 方法: {}", uri, method);

            // 记录请求参数
            Map<String, Object> requestParams = new HashMap<>();

            // 获取URL参数
            Enumeration<String> paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                String paramValue = request.getParameter(paramName);
                requestParams.put(paramName, paramValue);
            }

            // 如果是POST、PUT等可能包含请求体的请求，尝试获取请求体
            if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
                if (request.getContentType() != null &&
                        (request.getContentType().contains("application/json") ||
                                request.getContentType().contains("application/xml") ||
                                request.getContentType().contains("text/plain"))) {

                    try {
                        // 使用包装请求以允许多次读取请求体
                        CachedBodyHttpServletRequest cachedBodyRequest;

                        // 检查请求是否已经被包装
                        if (request instanceof CachedBodyHttpServletRequest) {
                            cachedBodyRequest = (CachedBodyHttpServletRequest) request;
                        } else {
                            cachedBodyRequest = new CachedBodyHttpServletRequest(request);
                            // 这里不需要重新赋值给request变量，而是直接传递包装后的请求
                        }

                        String requestBody = StreamUtils.copyToString(cachedBodyRequest.getInputStream(), StandardCharsets.UTF_8);
                        log.info("请求体: {}", requestBody);

                        // 将包装后的请求设置为请求属性，以便后续可以获取
                        request.setAttribute("cachedBodyRequest", cachedBodyRequest);

                    } catch (Exception e) {
                        log.warn("无法读取请求体: {}", e.getMessage());
                    }
                }
            }

            // 记录请求参数
            if (!requestParams.isEmpty()) {
                try {
                    log.info("请求参数: {}", objectMapper.writeValueAsString(requestParams));
                } catch (Exception e) {
                    log.warn("序列化请求参数失败: {}", e.getMessage());
                }
            }
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (handler instanceof HandlerMethod) {
            // 计算请求耗时
            Long startTime = (Long) request.getAttribute(timeAttribute);
            if (startTime != null) {
                long endTime = System.currentTimeMillis();
                long executionTime = endTime - startTime;

                log.info("请求结束 - URI: {}, 状态码: {}, 耗时: {}ms", request.getRequestURI(), response.getStatus(), executionTime);
            } else {
                log.info("请求结束 - URI: {}, 状态码: {}", request.getRequestURI(), response.getStatus());
            }
        }
    }
}
