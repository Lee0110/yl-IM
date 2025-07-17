package com.lyl.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 一致性哈希算法工具类
 */
public class ConsistentHashUtil {

    // 虚拟节点数量，增加虚拟节点可以让哈希环更加均匀
    private static final int VIRTUAL_NODES = 200;

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

        // 构建哈希环
        SortedMap<Long, String> hashRing = buildHashRing(instances);

        // 计算用户ID的哈希值
        long userHash = hash(userId);

        // 顺时针找到第一个大于等于用户哈希值的节点
        SortedMap<Long, String> tailMap = hashRing.tailMap(userHash);
        if (tailMap.isEmpty()) {
            // 如果没有，则取第一个节点
            return hashRing.get(hashRing.firstKey());
        } else {
            // 否则取最近的节点
            return hashRing.get(tailMap.firstKey());
        }
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

        // 使用前8个字节构造一个long值
        long hashCode = ((long) (digest[3] & 0xFF) << 24)
                | ((long) (digest[2] & 0xFF) << 16)
                | ((long) (digest[1] & 0xFF) << 8)
                | (digest[0] & 0xFF);

        return hashCode & 0xFFFFFFFFL; // 取正数
    }
}
