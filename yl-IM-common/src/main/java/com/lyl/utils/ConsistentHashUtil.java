package com.lyl.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ConsistentHashUtil {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private DiscoveryClient discoveryClient;

    @Value("${yl_IM.netty.server.name}")
    private String nettyServerName;

    // 虚拟节点数量 - 派蒙调优的黄金比例
    private static final int VIRTUAL_NODES = 160;

    // Redis缓存前缀
    private static final String REDIS_CACHE_KEY = "consistent_hash:ring";

    // Redis缓存过期时间（分钟）
    private static final int REDIS_CACHE_EXPIRE_MINUTES = 10;

    /**
     * 根据用户ID获取对应的netty服务实例
     *
     * @param userId 用户ID
     * @return 选中的服务实例
     */
    public String selectNettyServer(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        // 从缓存拿到一致性哈希环
        SortedMap<Long, String> hashRing = null;
        Object cachedValue = redisTemplate.opsForValue().get(REDIS_CACHE_KEY);
        if (cachedValue != null) {
            try {
                hashRing = (SortedMap<Long, String>) cachedValue;
            } catch (Exception e) {
                log.error("从Redis获取哈希环数据转换失败", e);
            }
        }

        if (hashRing == null || hashRing.isEmpty()) {
            // 如果缓存未命中，重新获取服务实例列表
            List<ServiceInstance> serviceInstanceList = discoveryClient.getInstances(nettyServerName);
            if (serviceInstanceList == null || serviceInstanceList.isEmpty()) {
                // 如果没有可用服务实例，抛出异常
                throw new RuntimeException("没有可用的服务实例：" + nettyServerName);
            }

            // 使用一致性哈希选择服务器
            Set<String> instances = serviceInstanceList.stream()
                    .map(instance -> instance.getHost() + ":" + instance.getPort())
                    .collect(Collectors.toSet());

            // 缓存未命中，构建新的哈希环
            hashRing = buildHashRing(instances);

            // 存储到Redis
            try {
                redisTemplate.opsForValue().set(
                        REDIS_CACHE_KEY,
                        hashRing,
                        REDIS_CACHE_EXPIRE_MINUTES,
                        TimeUnit.MINUTES
                );
            } catch (Exception e) {
                log.error("存储哈希环到Redis失败", e);
            }
        }

        // 计算用户ID的哈希值
        long userHash = hash(userId);

        // 顺时针找到第一个大于等于用户哈希值的节点
        SortedMap<Long, String> tailMap = hashRing.tailMap(userHash);
        if (tailMap.isEmpty()) {
            return hashRing.get(hashRing.firstKey());
        } else {
            return hashRing.get(tailMap.firstKey());
        }
    }

    /**
     * 根据用户ID获取对应的spring服务实例
     *
     * @param userId 用户ID
     * @return 选中的服务实例
     */
    public String selectSpringServer(String userId) {
        // netty服务和spring服务在同一个机器上，只不过端口不同
        String nettyServer = selectNettyServer(userId);
        if (nettyServer == null || nettyServer.isEmpty()) {
            return null;
        }
        String[] parts = nettyServer.split(":");
        if (parts.length != 2) {
            return null;
        }
        // spring服务的端口比netty的小1000
        return parts[0] + ":" + (Integer.parseInt(parts[1]) - 1000);
    }

    /**
     * 构建哈希环
     */
    private SortedMap<Long, String> buildHashRing(Collection<String> instances) {
        SortedMap<Long, String> hashRing = new TreeMap<>();

        for (String instance : instances) {
            // 为每个实例添加虚拟节点
            for (int i = 0; i < VIRTUAL_NODES; i++) {
                String virtualNode = instance + "#" + i;
                long hash = hash(virtualNode);
                hashRing.put(hash, instance);
            }
        }

        return hashRing;
    }

    /**
     * 手动清理缓存
     */
    public void clearCache() {
        log.info("清理哈希环缓存数据");
        Set<String> keys = redisTemplate.keys(REDIS_CACHE_KEY);
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * 计算哈希值，使用MD5
     */
    private static long hash(String key) {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("获取MD5实例失败", e);
        }

        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        md5.update(keyBytes);
        byte[] digest = md5.digest();

        long hashCode = ((long) (digest[3] & 0xFF) << 24)
                | ((long) (digest[2] & 0xFF) << 16)
                | ((long) (digest[1] & 0xFF) << 8)
                | (digest[0] & 0xFF);

        return hashCode & 0xFFFFFFFFL;
    }
}
