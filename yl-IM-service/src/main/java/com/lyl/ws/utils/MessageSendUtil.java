package com.lyl.ws.utils;

import com.alibaba.fastjson2.JSONObject;
import com.lyl.domain.dto.MessageDTO;
import com.lyl.ws.constant.KafkaTopicConstant;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

@Component
@Slf4j
public class MessageSendUtil {
    @Resource
    private LocalChannelStoreUtil localChannelStoreUtil;

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 发送消息到指定用户
     *
     * @param userId  接收者用户ID
     * @param messageDTO 消息内容
     * @param isBroadcast 如果本地没有对应用户的长连接，是否广播消息到Kafka
     */
    public void sendMessageToUser(Long userId, MessageDTO messageDTO, boolean isBroadcast) {
        if (Objects.isNull(userId) || Objects.isNull(messageDTO) || Objects.isNull(messageDTO.getContent())) {
            log.error("Invalid parameters: userId or message is null");
            return;
        }

        Channel channel = localChannelStoreUtil.getChannelByUserId(userId);
        String jsonString = JSONObject.toJSONString(messageDTO);
        if (Objects.nonNull(channel) && channel.isActive()) {
            channel.writeAndFlush(new TextWebSocketFrame(jsonString));
        } else {
            if (isBroadcast) {
                kafkaTemplate.send(KafkaTopicConstant.MESSAGE_TOPIC, userId.toString(), jsonString);
            }
        }
    }

    public void sendMessageToUser(Long userId, MessageDTO messageDTO) {
        sendMessageToUser(userId, messageDTO, true);
    }

    public void sendMessageToUser(MessageDTO messageDTO) {
        if (Objects.isNull(messageDTO)) {
            log.error("Invalid messageVO: null");
            return;
        }
        sendMessageToUser(messageDTO.getReceiverId(), messageDTO, true);
    }
}
