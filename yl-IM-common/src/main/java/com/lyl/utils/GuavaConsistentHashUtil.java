package com.lyl.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GuavaConsistentHashUtil {

    // 虚拟节点数量 - 派蒙调优的黄金比例
    private static final int VIRTUAL_NODES = 160;

    private static final Object CACHE_LOCK = new Object();

    // 哈希环缓存 - 派蒙的性能优化秘诀
    private static final Cache<String, SortedMap<Long, String>> HASH_RING_CACHE = CacheBuilder.newBuilder()
            .maximumSize(50)                          // 最大缓存50个不同的实例组合
            .expireAfterWrite(10, TimeUnit.MINUTES)     // 写入后10分钟过期
            .expireAfterAccess(10, TimeUnit.MINUTES)    // 访问后10分钟过期
            .recordStats()                             // 启用统计功能
            .build();

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
            // 只有一个实例，直接返回
            return instances.iterator().next();
        }

        // 获取或构建哈希环
        SortedMap<Long, String> hashRing = getOrBuildHashRing(instances);

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
     * 批量选择服务器
     */
    public static Map<String, String> batchSelectServers(Collection<String> userIds, Collection<String> instances) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }

        if (instances == null || instances.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, String> result = new HashMap<>(userIds.size());

        if (instances.size() == 1) {
            // 只有一个实例，全部路由到这个实例
            String singleInstance = instances.iterator().next();
            for (String userId : userIds) {
                if (userId != null && !userId.trim().isEmpty()) {
                    result.put(userId, singleInstance);
                }
            }
            return result;
        }

        try {
            SortedMap<Long, String> hashRing = getOrBuildHashRing(instances);

            for (String userId : userIds) {
                if (userId == null || userId.trim().isEmpty()) {
                    continue;
                }

                long userHash = hash(userId);
                // 顺时针找到第一个大于等于用户哈希值的节点
                SortedMap<Long, String> tailMap = hashRing.tailMap(userHash);
                String s;
                if (tailMap.isEmpty()) {
                    s = hashRing.get(hashRing.firstKey());
                } else {
                    s = hashRing.get(tailMap.firstKey());
                }
                result.put(userId, s);
            }

            return result;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * 获取或构建哈希环
     */
    private static SortedMap<Long, String> getOrBuildHashRing(Collection<String> instances) {
        // 生成实例列表的缓存键
        String cacheKey = generateCacheKey(instances);

        synchronized (CACHE_LOCK) {
            // 尝试从缓存获取
            SortedMap<Long, String> hashRing = HASH_RING_CACHE.getIfPresent(cacheKey);
            if (hashRing != null) {
                return hashRing;
            }

            // 缓存未命中，构建新的哈希环
            hashRing = buildHashRing(instances);
            HASH_RING_CACHE.put(cacheKey, hashRing);


            return hashRing;
        }
    }

    /**
     * 构建哈希环
     */
    private static SortedMap<Long, String> buildHashRing(Collection<String> instances) {
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
     * 生成缓存键
     */
    private static String generateCacheKey(Collection<String> instances) {
        // 将实例列表排序后生成缓存键，确保相同的实例集合有相同的缓存键
        return String.valueOf(hash(instances.stream().sorted().collect(Collectors.joining(","))));
    }

    /**
     * 获取缓存统计信息 - 派蒙的数据看板
     */
    public static Map<String, Object> getCacheStats() {
        CacheStats stats = HASH_RING_CACHE.stats();
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("当前缓存大小", HASH_RING_CACHE.size());
        result.put("命中率", String.format("%.2f%%", stats.hitRate() * 100));
        result.put("命中次数", stats.hitCount());
        result.put("未命中次数", stats.missCount());

        // 计算平均加载时间
        double avgLoadTime = stats.loadCount() > 0 ?
            (double) stats.totalLoadTime() / stats.loadCount() / 1_000_000.0 : 0.0;
        result.put("平均加载时间", String.format("%.2f ms", avgLoadTime));
        result.put("清理次数", stats.evictionCount());

        return result;
    }

    /**
     * 手动清理缓存
     */
    public static void clearCache() {
        HASH_RING_CACHE.invalidateAll();
    }

    /**
     * 获取缓存中的哈希环数量
     */
    public static long getCacheSize() {
        return HASH_RING_CACHE.size();
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
