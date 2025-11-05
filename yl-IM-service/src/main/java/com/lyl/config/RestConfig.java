package com.lyl.config;

import com.lyl.utils.MDCContextUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RestConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        RestTemplate restTemplate = new RestTemplate(factory);

        // 添加拦截器：为所有请求自动注入 traceId 请求头
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>(restTemplate.getInterceptors());
        interceptors.add((request, body, execution) -> {
            // 如果请求头里已有 traceId，就沿用；否则从当前 MDC 获取（必要时生成）并补齐
            String existing = request.getHeaders().getFirst("traceId");
            if (!StringUtils.hasText(existing)) {
                String traceId = MDCContextUtil.getTraceId();
                request.getHeaders().set("traceId", traceId);
            }
            return execution.execute(request, body);
        });
        restTemplate.setInterceptors(interceptors);
        return restTemplate;
    }
}
