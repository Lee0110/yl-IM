package com.lyl.filter;

import com.lyl.utils.GuavaConsistentHashUtil;
import lombok.Data;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于一致性哈希的负载均衡过滤器
 * 用于WebSocket连接的服务器选择
 */
@Component
public class ConsistentHashLoadBalancerFilter extends AbstractGatewayFilterFactory<ConsistentHashLoadBalancerFilter.Config> {

    private final DiscoveryClient discoveryClient;

    public ConsistentHashLoadBalancerFilter(DiscoveryClient discoveryClient) {
        super(Config.class);
        this.discoveryClient = discoveryClient;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String serviceName = config.getServiceName();
            String userIdParam = config.getUserIdParam();

            // 从请求中获取用户ID
            String userId = extractUserId(exchange.getRequest(), userIdParam);
            if (userId == null || userId.isEmpty()) {
                // 如果没有用户ID，抛出异常
                return Mono.error(new RuntimeException("未能获取用户ID，无法进行路由"));
            }

            // 获取服务实例列表
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
            if (instances == null || instances.isEmpty()) {
                // 如果没有可用服务实例，抛出异常
                return Mono.error(new RuntimeException("没有可用的服务实例：" + serviceName));
            }

            // 使用一致性哈希选择服务器
            List<String> instanceUrls = instances.stream()
                    .map(instance -> instance.getHost() + ":" + instance.getPort())
                    .collect(Collectors.toList());

            String selectedServerUrl = GuavaConsistentHashUtil.selectServer(userId, instanceUrls);

            if (selectedServerUrl != null) {
                // 获取原始请求的URI和查询参数
                ServerHttpRequest request = exchange.getRequest();
                URI originalUri = request.getURI();
                String query = originalUri.getRawQuery();

                // 构造新URI时保留原始查询参数
                URI newUri;
                if (query != null && !query.isEmpty()) {
                    newUri = URI.create("ws://" + selectedServerUrl + originalUri.getPath() + "?" + query);
                } else {
                    newUri = URI.create("ws://" + selectedServerUrl + originalUri.getPath());
                }

                exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, newUri);
            } else {
                // 如果没有选择出服务器，抛出异常
                return Mono.error(new RuntimeException("一致性哈希算法未能选择出合适的服务器"));
            }

            return chain.filter(exchange);
        };
    }

    /**
     * 从请求中提取用户ID
     */
    private String extractUserId(ServerHttpRequest request, String userIdParam) {
        // 首先尝试从查询参数中获取
        List<String> userIds = request.getQueryParams().get(userIdParam);
        if (userIds != null && !userIds.isEmpty()) {
            return userIds.get(0);
        }

        // 然后尝试从请求头中获取
        String userIdFromHeader = request.getHeaders().getFirst("userId");
        if (userIdFromHeader != null && !userIdFromHeader.isEmpty()) {
            return userIdFromHeader;
        }

        // 最后尝试从路径中提取（假设路径格式为 /ws/userId）
        String path = request.getPath().value();
        String[] pathSegments = path.split("/");
        if (pathSegments.length > 2) {
            return pathSegments[2]; // /ws/userId 格式
        }

        return null;
    }

    /**
     * 配置类
     */
    @Data
    public static class Config {
        private String serviceName = "yl-im-netty";
        private String userIdParam = "userId";
    }
}
