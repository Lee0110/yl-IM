package com.lyl.util;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 服务实例管理器，用于获取Nacos上的服务实例
 */
@Component
@Slf4j
public class ServerInstanceManager {

    private final NacosDiscoveryProperties nacosDiscoveryProperties;
    private static final String YL_IM_NETTY = "yl-im-netty"; // 与 NacosRegisterUtil 中的服务名一致

    @Autowired
    public ServerInstanceManager(NacosDiscoveryProperties nacosDiscoveryProperties) {
        this.nacosDiscoveryProperties = nacosDiscoveryProperties;
    }

    /**
     * 获取所有可用的Netty服务实例
     *
     * @return 服务实例列表，格式为 "ip:port"
     */
    @SuppressWarnings("deprecation")
    public List<String> getAllNettyInstances() {
        try {
            NamingService namingService = nacosDiscoveryProperties.namingServiceInstance();
            List<Instance> instances = namingService.getAllInstances(YL_IM_NETTY);
            
            List<String> result = instances.stream()
                    .filter(Instance::isEnabled)
                    .filter(Instance::isHealthy)
                    .map(instance -> instance.getIp() + ":" + instance.getPort())
                    .collect(Collectors.toList());
            
            log.info("获取到的Netty实例列表: {}", result);
            return result;
        } catch (NacosException e) {
            log.error("Failed to get netty instances from nacos", e);
            return Collections.emptyList();
        }
    }

    /**
     * 根据userId选择一个Netty服务实例
     *
     * @param userId 用户ID
     * @return 选中的服务实例，格式为 "ip:port"，如果没有可用实例则返回null
     */
    public String selectNettyInstance(String userId) {
        log.info("为用户 {} 选择Netty实例", userId);
        List<String> instances = getAllNettyInstances();
        if (instances.isEmpty()) {
            log.warn("No available netty instance found");
            return null;
        }
        
        String selected = ConsistentHashUtil.selectServer(userId, instances);
        log.info("为用户 {} 选择的Netty实例: {}", userId, selected);
        return selected;
    }
}
