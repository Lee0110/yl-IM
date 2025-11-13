package com.lyl.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyl.constant.RedisKeyConstant;
import com.lyl.service.message.IMessageService;
import com.lyl.service.message.dto.BroadcastMessageDTO;
import com.lyl.service.message.dto.MessageDTO;
import com.lyl.utils.MDCContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import javax.annotation.Resource;

@Configuration
@Slf4j
public class RedisConfig {

    @Resource
    @Lazy
    private IMessageService messageService;

    @Resource
    private ObjectMapper objectMapper;

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.addMessageListener(((message, pattern) -> {
                    String messageContent = new String(message.getBody());
                    log.info("接收到Redis广播消息: {}", messageContent);
                    try {
                        BroadcastMessageDTO messageDTO = objectMapper.readValue(messageContent, BroadcastMessageDTO.class);
                        MDCContextUtil.setTraceId(messageDTO.getTraceId());
                        log.info("处理广播消息: {}", messageContent);
                        // 尝试向本机连接的用户发送消息
                        messageService.sendMessageToLocalUser(messageDTO);
                    } catch (Exception e) {
                        log.error("处理广播消息异常", e);
                    }
                }),
                new ChannelTopic(RedisKeyConstant.MESSAGE_BROADCAST_CHANNEL));
        container.setConnectionFactory(connectionFactory);
        return container;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        // 使用通用的JSON序列化器来处理对象值
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
