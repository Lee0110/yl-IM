package com.lyl.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyl.utils.ConsistentHashUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class UtilConfig {

    @Value("${yl_IM.netty.server.name}")
    private String nettyServerName;

    @Bean
    public ConsistentHashUtil consistentHashUtil(RedisTemplate<String, String> redisTemplate,
                                                 DiscoveryClient discoveryClient,
                                                 ObjectMapper objectMapper) {
        return new ConsistentHashUtil(nettyServerName, discoveryClient, redisTemplate, objectMapper);
    }
}
