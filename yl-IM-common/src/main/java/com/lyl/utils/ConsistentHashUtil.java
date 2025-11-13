package com.lyl.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyl.constant.RedisKeyConstant;
import com.lyl.exception.IMException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.data.redis.core.RedisTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class ConsistentHashUtil {

    private final RedisTemplate<String, String> redisTemplate;

    private final DiscoveryClient discoveryClient;

    private final ObjectMapper objectMapper;

    /**
     * netty服务名称
     */
    private final String nettyServerName;

    /**
     * 虚拟节点数量 - 用于负载均衡
     */
    private final int virtualNodes;

    /**
     * Redis缓存过期时间（分钟）
     */
    private final int redisCacheExpireMinutes;

    public ConsistentHashUtil(String nettyServerName, DiscoveryClient discoveryClient, RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.nettyServerName = nettyServerName;
        this.discoveryClient = discoveryClient;
        this.redisTemplate = redisTemplate;
        this.virtualNodes = 160;
        this.redisCacheExpireMinutes = 10;
        this.objectMapper = objectMapper;
    }

    public ConsistentHashUtil(RedisTemplate<String, String> redisTemplate, DiscoveryClient discoveryClient, String nettyServerName, int virtualNodes, int redisCacheExpireMinutes, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.discoveryClient = discoveryClient;
        this.nettyServerName = nettyServerName;
        this.virtualNodes = virtualNodes;
        this.redisCacheExpireMinutes = redisCacheExpireMinutes;
        this.objectMapper = objectMapper;
    }

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
        String cachedValue = redisTemplate.opsForValue().get(RedisKeyConstant.CONSISTENT_HASH_RING);
        if (cachedValue != null) {
            try {
                hashRing = objectMapper.readValue(cachedValue, new TypeReference<SortedMap<Long, String>>() {
                });
            } catch (Exception e) {
                log.error("从Redis获取哈希环数据转换失败", e);
            }
        }

        if (hashRing == null || hashRing.isEmpty()) {
            // 如果缓存未命中，重新获取服务实例列表
            List<ServiceInstance> serviceInstanceList = discoveryClient.getInstances(nettyServerName);
            if (serviceInstanceList == null || serviceInstanceList.isEmpty()) {
                // 如果没有可用服务实例，抛出异常
                throw new IMException("没有可用的服务实例：" + nettyServerName);
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
                        RedisKeyConstant.CONSISTENT_HASH_RING,
                        objectMapper.writeValueAsString(hashRing),
                        redisCacheExpireMinutes,
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
            for (int i = 0; i < virtualNodes; i++) {
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
        Set<String> keys = redisTemplate.keys(RedisKeyConstant.CONSISTENT_HASH_RING);
        if (!keys.isEmpty()) {
            log.info("清理哈希环缓存数据");
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
            throw new IMException("获取MD5实例失败", e);
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
