package com.lyl.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 基于一致性哈希的负载均衡过滤器
 * 用于WebSocket连接的服务器选择
 */
@Slf4j
@Component
public class WebsocketGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    @Resource
    private CustomWebsocketGatewayFilter customWebsocketGatewayFilter;

    public WebsocketGatewayFilterFactory() {
        super(Object.class);
    }

    @Override
    public GatewayFilter apply(Object config) {
        return customWebsocketGatewayFilter;
    }
}
