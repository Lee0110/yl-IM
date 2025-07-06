package com.lyl.message_queue;

import com.alibaba.fastjson2.JSONObject;
import com.lyl.domain.dto.MessageDTO;
import com.lyl.ws.constant.KafkaTopicConstant;
import com.lyl.ws.utils.MessageSendUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class KafkaConsumer {
    @Resource
    private MessageSendUtil messageSendUtil;

    // 使用随机生成的groupId，来实现广播效果，但设置消费者从最新位置开始消费
    @KafkaListener(
        topics = KafkaTopicConstant.MESSAGE_TOPIC,
        groupId = "#{T(java.util.UUID).randomUUID().toString()}",
        properties = {"auto.offset.reset=latest"}  // 设置从最新位置开始消费
    )
    public void listen(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        String receiverId = record.key();
        String json = record.value();
        log.info("接收到kafka消息 receiverId: {}, Value: {}", receiverId, json);
        MessageDTO messageDTO = JSONObject.parseObject(json, MessageDTO.class);
        messageSendUtil.sendMessageToUser(Long.valueOf(receiverId), messageDTO, false);
        acknowledgment.acknowledge();
    }
}
