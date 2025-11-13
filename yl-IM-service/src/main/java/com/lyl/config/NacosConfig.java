package com.lyl.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.lyl.utils.ConsistentHashUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.Properties;

@Configuration
public class NacosConfig {
    @Value("${spring.cloud.nacos.discovery.server-addr}")
    private String nacosAddr;

    @Value("${yl_IM.netty.server.name}")
    private String nettyServerName;

    @Resource
    private ConsistentHashUtil consistentHashUtil;

    @Bean
    public NamingService namingService() {
        try {
            Properties properties = new Properties();
            properties.setProperty("serverAddr", nacosAddr);
            NamingService namingService = NacosFactory.createNamingService(properties);
            namingService.subscribe(nettyServerName, event -> consistentHashUtil.clearCache());
            return namingService;
        } catch (NacosException e) {
            throw new RuntimeException("Failed to create NamingService", e);
        }
    }
}
