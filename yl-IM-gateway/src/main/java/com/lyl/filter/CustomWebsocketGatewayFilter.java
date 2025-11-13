package com.lyl.filter;

import com.lyl.utils.ConsistentHashUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Slf4j
public class CustomWebsocketGatewayFilter implements GatewayFilter, Ordered {

    @Resource
    private ConsistentHashUtil consistentHashUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        // 从请求中获取用户ID
        String userId = extractUserId(exchange.getRequest());
        if (userId == null || userId.isEmpty()) {
            log.warn("缺少 userId 参数");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            byte[] bytes = "缺少 userId 参数".getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8");
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }

        // 简单校验与后端保持一致：1..1_000_000 视为合法
        try {
            long uid = Long.parseLong(userId);
            if (uid < 1 || uid > 1_000_000) {
                log.warn("非法的userId: {}", userId);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                byte[] bytes = "userId 非法，拒绝握手".getBytes(StandardCharsets.UTF_8);
                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
                exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8");
                return exchange.getResponse().writeWith(Mono.just(buffer));
            }
        } catch (NumberFormatException e) {
            log.warn("userId 格式错误: {}", userId);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            byte[] bytes = "userId 格式错误".getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8");
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }

        String selectedServerUrl = consistentHashUtil.selectNettyServer(userId);
        log.info("一致性哈希选择的服务器: {}, 用户id: {}", selectedServerUrl, userId);

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
    }

    /**
     * 从请求中提取用户ID
     */
    private String extractUserId(ServerHttpRequest request) {
        // 首先尝试从查询参数中获取
        List<String> userIds = request.getQueryParams().get("userId");
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


    @Override
    public int getOrder() {
        return RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 1;
    }
}
