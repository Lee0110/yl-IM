package com.lyl.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyl.domain.Result;
import com.lyl.exception.OcsErrorCode;
import com.lyl.exception.OcsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.annotation.Resource;

@ControllerAdvice
@Slf4j
public class GlobalResponseHandler implements ResponseBodyAdvice<Object> {
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 如果返回的类型已经是Result，或者方法或类上有IgnoreRestControllerResponseAdvice注解，则不进行处理
        return !returnType.hasMethodAnnotation(IgnoreRestControllerResponseAdvice.class)
                && !returnType.getDeclaringClass().isAnnotationPresent(IgnoreRestControllerResponseAdvice.class)
                && !Result.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        if (returnType.getParameterType().isAssignableFrom(void.class)) {
            return Result.success();
        }
        if (body instanceof String) {
            try {
                return objectMapper.writeValueAsString(Result.success(body));
            } catch (JsonProcessingException e) {
                log.error(e.getMessage(), e);
                throw new OcsException(OcsErrorCode.JSON_PARSE_ERROR);
            }
        }
        return Result.success(body);
    }
}
