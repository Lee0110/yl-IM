package com.lyl.config;

import com.lyl.loadbalancer.ConsistentHashLoadBalancer;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * 负载均衡器配置
 * 为特定服务配置一致性哈希负载均衡
 */
@Configuration
public class LoadBalancerConfig {

    /**
     * 为 yl-im-netty 服务配置一致性哈希负载均衡器
     */
    @Bean
    public ReactorLoadBalancer<ServiceInstance> consistentHashLoadBalancer(Environment environment,
                                                                          LoadBalancerClientFactory loadBalancerClientFactory) {
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);

        // 只对 WebSocket 服务使用一致性哈希
        if ("yl-im-netty".equals(name)) {
            return new ConsistentHashLoadBalancer(
                    loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class),
                    name);
        }

        // 其他服务使用默认负载均衡
        return null;
    }
}
