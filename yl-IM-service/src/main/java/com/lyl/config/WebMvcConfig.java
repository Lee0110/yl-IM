package com.lyl.config;

import com.lyl.interceptor.RequestResponseLogInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册请求响应日志拦截器
        registry.addInterceptor(new RequestResponseLogInterceptor())
                .addPathPatterns("/**");
    }
}
