package com.lyl.service.message;

import com.lyl.service.message.dto.MessageDTO;

/**
 * 消息处理器接口
 * 定义消息处理的通用方法
 */
public interface IMessageHandler {

    /**
     * 处理消息
     *
     * @param messageDTO 消息数据传输对象
     */
    void handleMessage(MessageDTO messageDTO);
}
