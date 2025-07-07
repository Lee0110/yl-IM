package com.lyl.ws.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RedisUtil {

    private static final String USER_SERVER_KEY_PREFIX = "im:user:server:";
    private static final long USER_SERVER_EXPIRE_TIME = 24 * 60 * 60; // 24小时

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 存储用户ID与服务器ID的映射
     *
     * @param userId   用户ID
     * @param serverId 服务器ID (IP:端口)
     */
    public void saveUserServerMapping(Long userId, String serverId) {
        String key = USER_SERVER_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, serverId, USER_SERVER_EXPIRE_TIME, TimeUnit.SECONDS);
        log.info("保存用户[{}]与服务器[{}]的映射关系", userId, serverId);
    }

    /**
     * 获取用户对应的服务器ID
     *
     * @param userId 用户ID
     * @return 服务器ID (IP:端口)
     */
    public String getUserServerMapping(Long userId) {
        String key = USER_SERVER_KEY_PREFIX + userId;
        String serverId = redisTemplate.opsForValue().get(key);
        if (serverId != null) {
            log.info("获取到用户[{}]的服务器映射[{}]", userId, serverId);
        }
        return serverId;
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
}
