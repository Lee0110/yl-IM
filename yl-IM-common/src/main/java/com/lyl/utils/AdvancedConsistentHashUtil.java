package com.lyl.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 高性能一致性哈希算法工具类
 * 支持本地缓存 + 定时清理过期缓存
 * 无外部依赖，开箱即用
 */
public class AdvancedConsistentHashUtil {

    // 虚拟节点数量
    private static final int VIRTUAL_NODES = 200;
    
    // 本地缓存
    private static final Map<String, CachedHashRing> CACHE = new ConcurrentHashMap<>();
    
    // 缓存过期时间（毫秒）
    private static final long CACHE_EXPIRE_TIME = 300_000; // 5分钟
    
    // 最大缓存数量
    private static final int MAX_CACHE_SIZE = 200;
    
    // 定时清理器
    private static final ScheduledExecutorService CLEANUP_EXECUTOR = 
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ConsistentHash-Cache-Cleanup");
            t.setDaemon(true);
            return t;
        });

    static {
        // 启动定时清理任务，每分钟清理一次过期缓存
        CLEANUP_EXECUTOR.scheduleAtFixedRate(AdvancedConsistentHashUtil::cleanupExpiredCache, 
            60, 60, TimeUnit.SECONDS);
    }

    /**
     * 缓存的哈希环数据结构
     */
    private static class CachedHashRing {
        private final SortedMap<Long, String> hashRing;
        private final long createTime;
        private volatile long lastAccessTime;

        public CachedHashRing(SortedMap<Long, String> hashRing) {
            this.hashRing = hashRing;
            this.createTime = System.currentTimeMillis();
            this.lastAccessTime = createTime;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - createTime > CACHE_EXPIRE_TIME;
        }

        public SortedMap<Long, String> getHashRing() {
            this.lastAccessTime = System.currentTimeMillis();
            return hashRing;
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }
    }

    /**
     * 根据用户ID获取对应的服务实例
     *
     * @param userId 用户ID
     * @param instances 服务实例列表
     * @return 选中的服务实例
     */
    public static String selectServer(String userId, Collection<String> instances) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }

        if (instances.size() == 1) {
            return instances.iterator().next();
        }

        // 获取哈希环（使用缓存）
        SortedMap<Long, String> hashRing = getHashRingWithCache(instances);

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
     * 获取哈希环，优先从缓存获取
     *
     * @param instances 服务实例列表
     * @return 哈希环
     */
    private static SortedMap<Long, String> getHashRingWithCache(Collection<String> instances) {
        String cacheKey = calculateCacheKey(instances);
        
        // 从缓存获取
        CachedHashRing cached = CACHE.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.getHashRing();
        }
        
        // 缓存未命中或已过期，构建新的哈希环
        SortedMap<Long, String> hashRing = buildHashRing(instances);
        
        // 缓存大小控制
        if (CACHE.size() >= MAX_CACHE_SIZE) {
            evictLRUCache();
        }
        
        // 存入缓存
        CACHE.put(cacheKey, new CachedHashRing(hashRing));
        
        return hashRing;
    }

    /**
     * LRU策略淘汰缓存
     */
    private static void evictLRUCache() {
        // 找出最久未访问的缓存项并删除
        String oldestKey = null;
        long oldestTime = Long.MAX_VALUE;
        
        for (Map.Entry<String, CachedHashRing> entry : CACHE.entrySet()) {
            long accessTime = entry.getValue().getLastAccessTime();
            if (accessTime < oldestTime) {
                oldestTime = accessTime;
                oldestKey = entry.getKey();
            }
        }
        
        if (oldestKey != null) {
            CACHE.remove(oldestKey);
        }
    }

    /**
     * 定时清理过期缓存
     */
    private static void cleanupExpiredCache() {
        CACHE.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * 计算实例列表的缓存key
     *
     * @param instances 实例列表
     * @return 缓存key
     */
    private static String calculateCacheKey(Collection<String> instances) {
        List<String> sortedInstances = new ArrayList<>(instances);
        Collections.sort(sortedInstances);
        return "hash_" + Math.abs(sortedInstances.hashCode());
    }

    /**
     * 构建哈希环
     *
     * @param instances 服务实例列表
     * @return 哈希环
     */
    private static SortedMap<Long, String> buildHashRing(Collection<String> instances) {
        SortedMap<Long, String> hashRing = new TreeMap<>();

        for (String instance : instances) {
            for (int i = 0; i < VIRTUAL_NODES; i++) {
                String virtualNode = instance + "#" + i;
                long hash = hash(virtualNode);
                hashRing.put(hash, instance);
            }
        }

        return hashRing;
    }

    /**
     * 计算哈希值，使用MD5
     *
     * @param key 键
     * @return 哈希值
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

    /**
     * 清空所有缓存
     */
    public static void clearAllCache() {
        CACHE.clear();
    }

    /**
     * 清空特定实例列表的缓存
     *
     * @param instances 实例列表
     */
    public static void clearCache(Collection<String> instances) {
        String cacheKey = calculateCacheKey(instances);
        CACHE.remove(cacheKey);
    }

    /**
     * 预热缓存
     * 提前构建常用实例列表的哈希环
     *
     * @param instancesList 实例列表的列表
     */
    public static void warmupCache(List<Collection<String>> instancesList) {
        for (Collection<String> instances : instancesList) {
            if (instances != null && !instances.isEmpty()) {
                getHashRingWithCache(instances);
            }
        }
    }

    /**
     * 获取缓存统计信息
     *
     * @return 缓存统计
     */
    public static Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", CACHE.size());
        stats.put("maxCacheSize", MAX_CACHE_SIZE);
        stats.put("cacheExpireTime", CACHE_EXPIRE_TIME);
        
        // 统计过期的缓存数量
        long expiredCount = CACHE.values().stream()
            .mapToLong(cached -> cached.isExpired() ? 1 : 0)
            .sum();
        stats.put("expiredCacheCount", expiredCount);
        
        return stats;
    }

    /**
     * 关闭工具类，清理资源
     */
    public static void shutdown() {
        CLEANUP_EXECUTOR.shutdown();
        CACHE.clear();
    }
}
