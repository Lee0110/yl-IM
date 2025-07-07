package com.lyl.ws.utils;

import com.alibaba.fastjson2.JSONObject;
import com.lyl.domain.dto.MessageDTO;
import com.lyl.ws.service.RemoteMessageService;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

@Component
@Slf4j
public class MessageSendUtil {
    @Resource
    private LocalChannelStoreUtil localChannelStoreUtil;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private RemoteMessageService remoteMessageService;

    /**
     * 发送消息到指定用户
     *
     * @param userId  接收者用户ID
     * @param messageDTO 消息内容
     */
    public void sendMessageToUser(Long userId, MessageDTO messageDTO) {
        sendMessageToUser(userId, messageDTO, true);
    }

    public void sendMessageToUser(Long userId, MessageDTO messageDTO, boolean isRedirect) {
        if (Objects.isNull(userId) || Objects.isNull(messageDTO) || Objects.isNull(messageDTO.getContent())) {
            log.error("Invalid parameters: userId or message is null");
            return;
        }

        Channel channel = localChannelStoreUtil.getChannelByUserId(userId);
        String jsonString = JSONObject.toJSONString(messageDTO);
        if (Objects.nonNull(channel) && channel.isActive()) {
            // 用户连接在本机，直接发送
            channel.writeAndFlush(new TextWebSocketFrame(jsonString));
            log.info("本地发送消息给用户 {}：{}", userId, jsonString);
        } else {
            if (isRedirect) {
                // 用户不在本机，尝试通过Redis查找用户所在的服务器
                String serverIpPort = redisUtil.getUserServerMapping(userId);

                if (StringUtils.isNotEmpty(serverIpPort)) {
                    // 找到用户所在服务器，进行远程调用
                    log.info("用户 {} 不在本机，转发消息到服务器 {}", userId, serverIpPort);
                    boolean success = remoteMessageService.sendMessageToRemoteServer(serverIpPort, messageDTO);

                    if (!success) {
                        log.error("消息转发失败，用户 {} 可能已离线", userId);
                        // 这里可以添加消息离线存储逻辑
                    }
                } else {
                    log.error("未找到用户 {} 的服务器映射，用户可能已离线", userId);
                    // 这里可以添加消息离线存储逻辑
                }
            }
        }
    }

    public void sendMessageToUser(MessageDTO messageDTO) {
        if (Objects.isNull(messageDTO)) {
            log.error("Invalid messageVO: null");
            return;
        }
        sendMessageToUser(messageDTO.getReceiverId(), messageDTO);
    }
}
