package com.lyl.loadbalancer;

import com.lyl.utils.GuavaConsistentHashUtil;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于一致性哈希的负载均衡器
 * 用于WebSocket连接的服务实例选择
 */
public class ConsistentHashLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;
    private final String serviceId;

    public ConsistentHashLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
                                     String serviceId) {
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
        this.serviceId = serviceId;
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider
                .getIfAvailable(NoopServiceInstanceListSupplier::new);

        return supplier.get(request).next()
                .map(serviceInstances -> processInstanceResponse(supplier, serviceInstances, request));
    }

    private Response<ServiceInstance> processInstanceResponse(ServiceInstanceListSupplier supplier,
                                                             List<ServiceInstance> serviceInstances,
                                                             Request request) {
        if (serviceInstances.isEmpty()) {
            return new EmptyResponse();
        }

        if (serviceInstances.size() == 1) {
            return new DefaultResponse(serviceInstances.get(0));
        }

        // 提取用户ID
        String userId = extractUserId(request);
        if (userId == null || userId.isEmpty()) {
            // 如果没有用户ID，返回第一个实例
            return new DefaultResponse(serviceInstances.get(0));
        }

        // 使用一致性哈希选择服务实例
        List<String> instanceKeys = serviceInstances.stream()
                .map(instance -> instance.getHost() + ":" + instance.getPort())
                .collect(Collectors.toList());

        String selectedKey = GuavaConsistentHashUtil.selectServer(userId, instanceKeys);

        if (selectedKey != null) {
            ServiceInstance selectedInstance = serviceInstances.stream()
                    .filter(instance -> (instance.getHost() + ":" + instance.getPort()).equals(selectedKey))
                    .findFirst()
                    .orElse(serviceInstances.get(0));

            return new DefaultResponse(selectedInstance);
        }

        return new DefaultResponse(serviceInstances.get(0));
    }

    /**
     * 从请求中提取用户ID
     */
    private String extractUserId(Request request) {
        if (request.getContext() instanceof ServerWebExchange) {
            ServerWebExchange exchange = (ServerWebExchange) request.getContext();

            // 1. 尝试从查询参数中获取userId
            List<String> userIds = exchange.getRequest().getQueryParams().get("userId");
            if (userIds != null && !userIds.isEmpty()) {
                return userIds.get(0);
            }

            // 2. 尝试从请求头中获取
            String userIdFromHeader = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userIdFromHeader != null && !userIdFromHeader.isEmpty()) {
                return userIdFromHeader;
            }

            // 3. 尝试从路径中提取（例如：/ws/123456）
            String path = exchange.getRequest().getPath().value();
            String[] pathSegments = path.split("/");
            if (pathSegments.length > 2 && !"ws".equals(pathSegments[2])) {
                return pathSegments[2];
            }
        }

        return null;
    }

    private static class NoopServiceInstanceListSupplier implements ServiceInstanceListSupplier {
        @Override
        public String getServiceId() {
            return null;
        }

        @Override
        public Flux<List<ServiceInstance>> get() {
            return Flux.empty();
        }

        @Override
        public Flux<List<ServiceInstance>> get(Request request) {
            return Flux.empty();
        }
    }
}
