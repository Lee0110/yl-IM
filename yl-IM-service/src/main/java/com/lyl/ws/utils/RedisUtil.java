package com.lyl.ws.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class RedisUtil {

    private static final String USER_SERVER_KEY_PREFIX = "im:user:server:";
    private static final long USER_SERVER_EXPIRE_TIME = 24 * 60 * 60; // 24小时

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Value("${server.port}")
    private Integer serverPort;

    @Resource
    private LocalChannelStoreUtil localChannelStoreUtil;

    /**
     * 存储用户ID与本机服务器IP:HOST的映射
     *
     * @param userId   用户ID
     */
    public void saveUserServerMapping(Long userId) {
        String ip;
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.error("获取本机IP地址失败", e);
            throw new RuntimeException("获取本机IP地址失败", e);
        }

        String ipPort = ip + ":" + serverPort;

        String key = USER_SERVER_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, ipPort, USER_SERVER_EXPIRE_TIME, TimeUnit.SECONDS);
        log.info("保存用户[{}]与服务器[{}]的映射关系", userId, ipPort);
    }

    /**
     * 获取用户对应的服务器ID
     *
     * @param userId 用户ID
     * @return 服务器ID (IP:端口)
     */
    public String getUserServerMapping(Long userId) {
        String key = USER_SERVER_KEY_PREFIX + userId;
        String ipPort = redisTemplate.opsForValue().get(key);
        if (ipPort != null) {
            log.info("获取到用户[{}]的服务器映射[{}]", userId, ipPort);
        }
        return ipPort;
    }

    /**
     * 删除用户与服务器的映射关系
     *
     * @param userId 用户ID
     */
    public void removeUserServerMapping(Long userId) {
        String key = USER_SERVER_KEY_PREFIX + userId;
        redisTemplate.delete(key);
        log.info("删除用户[{}]的服务器映射", userId);
    }

    /**
     * 清除所有本地用户与服务器的映射关系
     */
    public void clearLocalUserServerMappings() {
        Set<Long> allUserIdList = localChannelStoreUtil.getAllUserIds();
        Set<String> needDeleteKeyList = allUserIdList.stream().map(userId -> USER_SERVER_KEY_PREFIX + userId).collect(Collectors.toSet());
        redisTemplate.delete(needDeleteKeyList);
        log.info("清除本地用户与服务器的映射关系, 共删除 {} 条, 明细如下：{}", needDeleteKeyList.size(), allUserIdList);
    }
}
