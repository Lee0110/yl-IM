package com.lyl.utils;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 一致性哈希工具类性能测试
 */
public class ConsistentHashPerformanceTest {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 一致性哈希工具类性能测试 ===\n");
        
        // 准备测试数据
        List<String> instances = Arrays.asList(
                "server" + UUID.randomUUID() + ":8080",
                "server" + UUID.randomUUID() + ":8080",
                "server" + UUID.randomUUID() + ":8080",
                "server" + UUID.randomUUID() + ":8080",
                "server" + UUID.randomUUID() + ":8080",
                "server" + UUID.randomUUID() + ":8080",
                "server" + UUID.randomUUID() + ":8080",
                "server" + UUID.randomUUID() + ":8080",
                "server" + UUID.randomUUID() + ":8080",
                "server" + UUID.randomUUID() + ":8080",
                "server" + UUID.randomUUID() + ":8080",
                "server" + UUID.randomUUID() + ":8080",
                "server" + UUID.randomUUID() + ":8080",
                "server" + UUID.randomUUID() + ":8080",
                "server" + UUID.randomUUID() + ":8080",
                "server" + UUID.randomUUID() + ":8080"
        );
        
        List<String> userIds = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            userIds.add("user_" + i);
        }

        System.out.println("测试场景：");
        System.out.println("- 服务实例数：" + instances.size());
        System.out.println("- 用户ID数量：" + userIds.size());
        System.out.println("- 每个测试循环：10000次调用");
        System.out.println("- 线程数：10\n");

        // 测试原始版本
        testCachedVersion(instances, userIds);
        
        // 测试高级版本
        testAdvancedVersion(instances, userIds);
        
        // 测试负载均衡效果
        testLoadBalancing(instances, userIds);
        
        // 清理资源
        AdvancedConsistentHashUtil.shutdown();
    }

    /**
     * 测试原始性能
     */
    private static void testCachedVersion(List<String> instances, List<String> userIds) 
            throws InterruptedException {
        System.out.println("🚀 测试原始版本...");
        
        long startTime = System.currentTimeMillis();
        
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < 1000; i++) {
                        String userId = userIds.get(i % userIds.size());
                        String server = ConsistentHashUtil.selectServer(userId, instances);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        long endTime = System.currentTimeMillis();
        System.out.println("✅ 原始版本耗时：" + (endTime - startTime) + "ms");
    }

    /**
     * 测试高级版本性能
     */
    private static void testAdvancedVersion(List<String> instances, List<String> userIds) 
            throws InterruptedException {
        System.out.println("⚡ 测试高级版本（LRU + 定时清理）...");
        
        long startTime = System.currentTimeMillis();
        
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < 1000; i++) {
                        String userId = userIds.get(i % userIds.size());
                        String server = AdvancedConsistentHashUtil.selectServer(userId, instances);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        long endTime = System.currentTimeMillis();
        System.out.println("✅ 高级版本耗时：" + (endTime - startTime) + "ms");
        
        Map<String, Object> stats = AdvancedConsistentHashUtil.getCacheStats();
        System.out.println("📊 缓存统计：" + stats + "\n");
    }

    /**
     * 测试负载均衡效果
     */
    private static void testLoadBalancing(List<String> instances, List<String> userIds) {
        System.out.println("⚖️ 测试负载均衡效果...");
        
        Map<String, Integer> distribution = new HashMap<>();
        
        for (String userId : userIds) {
            String server = AdvancedConsistentHashUtil.selectServer(userId, instances);
            distribution.merge(server, 1, Integer::sum);
        }
        
        System.out.println("负载分布：");
        distribution.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                double percentage = (entry.getValue() * 100.0) / userIds.size();
                System.out.printf("  %s: %d次 (%.1f%%)\n", 
                    entry.getKey(), entry.getValue(), percentage);
            });
        
        // 计算标准差，评估均衡程度
        double avg = userIds.size() / (double) instances.size();
        double variance = distribution.values().stream()
            .mapToDouble(count -> Math.pow(count - avg, 2))
            .average()
            .orElse(0.0);
        double stdDev = Math.sqrt(variance);
        
        System.out.printf("📈 均衡度统计：平均 %.1f次，标准差 %.2f\n", avg, stdDev);
        System.out.println("   (标准差越小表示负载越均衡)\n");
    }
}
