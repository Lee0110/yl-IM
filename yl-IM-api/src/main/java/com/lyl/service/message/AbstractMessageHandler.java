package com.lyl.service.message;

import com.lyl.service.message.dto.MessageDTO;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

/**
 * 抽象消息处理器
 * 提供消息处理的通用逻辑
 */
@Slf4j
public abstract class AbstractMessageHandler implements IMessageHandler {
    @Resource
    protected IMessageService messageService;

    @Override
    public void handleMessage(MessageDTO messageDTO) {
        // 处理消息业务逻辑
        processMessage(messageDTO);
    }

    /**
     * 处理消息业务逻辑(由子类实现)
     *
     * @param messageDTO 消息对象
     */
    protected abstract void processMessage(MessageDTO messageDTO);
}
