package com.lyl.ws.utils;

import com.alibaba.fastjson2.JSONObject;
import com.lyl.domain.dto.MessageDTO;
import com.lyl.utils.ConsistentHashUtil;
import com.lyl.constant.RedisKeyConstant;
import com.lyl.ws.service.RemoteMessageService;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

@Component
@Slf4j
public class MessageSendUtil {
    @Resource
    private LocalChannelStoreUtil localChannelStoreUtil;

    @Resource
    private ConsistentHashUtil consistentHashUtil;

    @Resource
    private RemoteMessageService remoteMessageService;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 发送消息给指定用户（本机 -> 一致性哈希选择服务器 -> 广播）
     *
     * @param messageDTO 消息内容
     */
    public void sendMessageToUser(MessageDTO messageDTO) {
        if (Objects.isNull(messageDTO) || Objects.isNull(messageDTO.getReceiverId())) {
            log.error("Invalid messageDTO: null or receiverId is null");
            throw new IllegalArgumentException("Invalid messageDTO: null or receiverId is null");
        }
        // 1. 先尝试发送给本机用户
        boolean isSendMessageToLocalUserSuccess = sendMessageToLocalUser(messageDTO);
        if (isSendMessageToLocalUserSuccess) {
            return;
        }

        // 2. 本机没有该用户连接，使用一致性哈希工具找到用户所在的服务器
        boolean isSendMessageToRemoteServerSuccess = sendMessageToSelectedServer(messageDTO);
        if (isSendMessageToRemoteServerSuccess) {
            return;
        }

        // 3. 服务器也没有该用户连接，进行广播
        broadcastMessage(messageDTO);
    }

    /**
     * 发送消息到选定的服务器，通过一致性哈希算法选择服务器
     *
     * @param messageDTO 消息内容
     * @return 是否发送成功
     */
    public boolean sendMessageToSelectedServer(MessageDTO messageDTO) {
        if (Objects.isNull(messageDTO) || Objects.isNull(messageDTO.getReceiverId())) {
            log.error("Invalid messageDTO: null or receiverId is null");
            return false;
        }
        Long userId = messageDTO.getReceiverId();
        String serverIpPort = consistentHashUtil.selectSpringServer(String.valueOf(userId));
        if (StringUtils.isEmpty(serverIpPort)) {
            throw new IllegalStateException("系统异常，未找到用户 " + userId + " 的服务器映射");
        }
        if (StringUtils.isEmpty(serverIpPort)) {
            log.error("Invalid serverIpPort: null or empty");
            return false;
        }
        log.info("发送消息到其他实例 {}: {}", serverIpPort, JSONObject.toJSONString(messageDTO));
        return remoteMessageService.sendMessageToRemoteServer(serverIpPort, messageDTO);
    }

    /**
     * 广播消息给所有服务实例
     *
     * @param messageDTO 消息内容
     */
    public void broadcastMessage(MessageDTO messageDTO) {
        if (Objects.isNull(messageDTO) || Objects.isNull(messageDTO.getReceiverId())) {
            log.error("Invalid messageDTO: null or receiverId is null");
            return;
        }
        log.info("广播消息给所有服务实例: {}", JSONObject.toJSONString(messageDTO));
        redisTemplate.convertAndSend(RedisKeyConstant.MESSAGE_BROADCAST_CHANNEL, JSONObject.toJSONString(messageDTO));
    }

    /**
     * 发送消息到本机连接的用户
     *
     * @param messageDTO 消息内容
     */
    public boolean sendMessageToLocalUser(MessageDTO messageDTO) {
        if (Objects.isNull(messageDTO) || Objects.isNull(messageDTO.getReceiverId())) {
            log.error("Invalid messageDTO: null or receiverId is null");
            return false;
        }
        Long userId = messageDTO.getReceiverId();
        Channel channel = localChannelStoreUtil.getChannelByUserId(userId);
        if (Objects.isNull(channel) || !channel.isActive()) {
            log.info("该实例没有连接的用户: {}", userId);
            return false;
        }
        String jsonString = JSONObject.toJSONString(messageDTO);
        channel.writeAndFlush(new TextWebSocketFrame(jsonString));
        log.info("本地发送消息给用户 {}：{}", userId, jsonString);
        return true;
    }
}
