package com.lyl.service.message;

import com.alibaba.fastjson2.JSONObject;
import com.lyl.constant.RedisKeyConstant;
import com.lyl.domain.dto.MessageDTO;
import com.lyl.utils.ConsistentHashUtil;
import com.lyl.utils.LocalChannelStoreUtil;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


import javax.annotation.Resource;
import java.util.Objects;

@Service
@Slf4j
public class MessageService implements IMessageService {

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Resource
    private ConsistentHashUtil consistentHashUtil;

    @Resource
    private LocalChannelStoreUtil localChannelStoreUtil;

    @Override
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

    @Override
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
        return sendMessageToRemoteServer(serverIpPort, messageDTO);
    }

    @Override
    public boolean sendMessageToRemoteServer(String serverIpPort, MessageDTO messageDTO) {
        try {
            String url = "http://" + serverIpPort + "/message/send";
            log.info("准备发送远程消息，URL: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<MessageDTO> request = new HttpEntity<>(messageDTO, headers);

            Boolean response = restTemplate.postForObject(url, request, Boolean.class);
            log.info("远程消息转发成功，服务器: {}，响应: {}", serverIpPort, response);
            return Boolean.TRUE.equals(response);
        } catch (Exception e) {
            log.error("远程消息转发失败，服务器: {}，错误: {}", serverIpPort, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void broadcastMessage(MessageDTO messageDTO) {
        if (Objects.isNull(messageDTO) || Objects.isNull(messageDTO.getReceiverId())) {
            log.error("Invalid messageDTO: null or receiverId is null");
            return;
        }
        log.info("广播消息给所有服务实例: {}", JSONObject.toJSONString(messageDTO));
        redisTemplate.convertAndSend(RedisKeyConstant.MESSAGE_BROADCAST_CHANNEL, JSONObject.toJSONString(messageDTO));
    }

    @Override
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
