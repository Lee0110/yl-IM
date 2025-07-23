package com.lyl.controller;

import com.lyl.utils.GuavaConsistentHashUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 一致性哈希测试控制器
 * 用于测试和验证一致性哈希路由功能
 */
@RestController
public class ConsistentHashTestController {

    @Autowired
    private DiscoveryClient discoveryClient;

    /**
     * 测试一致性哈希路由
     * 访问示例：http://localhost:10001/test-hash?userId=12345&serviceName=yl-im-netty
     */
    @GetMapping("/test-hash")
    public Map<String, Object> testConsistentHash(@RequestParam String userId,
                                                  @RequestParam(defaultValue = "yl-im-netty") String serviceName) {
        Map<String, Object> result = new HashMap<>();

        // 获取服务实例列表
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        List<String> instanceUrls = instances.stream()
                .map(instance -> instance.getHost() + ":" + instance.getPort())
                .collect(Collectors.toList());

        if (instanceUrls.isEmpty()) {
            result.put("error", "没有找到可用的服务实例: " + serviceName);
            return result;
        }

        // 使用一致性哈希选择服务器
        String selectedServer = GuavaConsistentHashUtil.selectServer(userId, instanceUrls);

        result.put("userId", userId);
        result.put("serviceName", serviceName);
        result.put("availableInstances", instanceUrls);
        result.put("selectedInstance", selectedServer);
        result.put("totalInstances", instanceUrls.size());

        return result;
    }

    /**
     * 批量测试一致性哈希分布
     * 访问示例：http://localhost:10001/test-distribution?serviceName=yl-im-netty
     */
    @GetMapping("/test-distribution")
    public Map<String, Object> testDistribution(@RequestParam(defaultValue = "yl-im-netty") String serviceName,
                                               @RequestParam(defaultValue = "100") int userCount) {
        Map<String, Object> result = new HashMap<>();

        // 获取服务实例列表
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        List<String> instanceUrls = instances.stream()
                .map(instance -> instance.getHost() + ":" + instance.getPort())
                .collect(Collectors.toList());

        if (instanceUrls.isEmpty()) {
            result.put("error", "没有找到可用的服务实例: " + serviceName);
            return result;
        }

        // 生成测试用户ID并统计分布
        Map<String, Integer> distribution = new HashMap<>();
        for (String instance : instanceUrls) {
            distribution.put(instance, 0);
        }

        for (int i = 1; i <= userCount; i++) {
            String userId = "user" + i;
            String selectedServer = GuavaConsistentHashUtil.selectServer(userId, instanceUrls);
            distribution.put(selectedServer, distribution.get(selectedServer) + 1);
        }

        result.put("serviceName", serviceName);
        result.put("totalUsers", userCount);
        result.put("availableInstances", instanceUrls);
        result.put("distribution", distribution);

        // 计算分布均匀性
        double avgUsers = (double) userCount / instanceUrls.size();
        double maxDeviation = distribution.values().stream()
                .mapToDouble(count -> Math.abs(count - avgUsers))
                .max().orElse(0);
        result.put("averageUsersPerInstance", avgUsers);
        result.put("maxDeviationFromAverage", maxDeviation);

        return result;
    }
}
