package com.lyl.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.annotation.Resource;

/**
 * 响应体日志记录组件
 * 用于记录所有Controller返回的响应体内容
 * 使用Order注解确保在GlobalResponseHandler之后执行，这样记录的是最终响应给客户端的内容
 */
@ControllerAdvice
@Slf4j
@Order // 设置最低优先级，确保在所有其他ResponseBodyAdvice之后执行
public class ResponseLogAdvice implements ResponseBodyAdvice<Object> {

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 对所有响应进行处理
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                 Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                 ServerHttpRequest request, ServerHttpResponse response) {
        // 记录响应体内容
        try {
            String requestPath = request.getURI().getPath();
            if (body != null) {
                String responseBody = objectMapper.writeValueAsString(body);
                // 避免日志内容过长
                if (responseBody.length() > 1000) {
                    log.info("响应体 [{}]: {}... (已截断)", requestPath, responseBody.substring(0, 1000));
                } else {
                    log.info("响应体 [{}]: {}", requestPath, responseBody);
                }
            } else {
                log.info("响应体 [{}]: null", requestPath);
            }
        } catch (JsonProcessingException e) {
            log.warn("记录响应体失败: {}", e.getMessage());
        }

        // 不改变原响应
        return body;
    }
}
