package com.lyl.controller;

import com.lyl.domain.dto.MessageDTO;
import com.lyl.ws.utils.MessageSendUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@Slf4j
@RequestMapping("message")
public class MessageController {

    @Resource
    private MessageSendUtil messageSendUtil;

    @PostMapping("send")
    public boolean sendMessage(@RequestBody MessageDTO messageDTO) {
        log.info("接口收到消息发送请求: {}", messageDTO);
        messageSendUtil.sendMessageToUser(messageDTO.getReceiverId(), messageDTO, false);
        return true;
    }
}
