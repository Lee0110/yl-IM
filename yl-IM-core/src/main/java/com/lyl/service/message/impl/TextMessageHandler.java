package com.lyl.service.message.impl;

import com.lyl.service.message.AbstractMessageHandler;
import com.lyl.service.message.dto.MessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TextMessageHandler extends AbstractMessageHandler {

    @Override
    protected void processMessage(MessageDTO messageDTO) {
        messageService.sendMessageToUser(messageDTO);
    }
}
