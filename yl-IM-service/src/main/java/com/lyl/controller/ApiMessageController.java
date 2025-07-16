package com.lyl.controller;

import com.lyl.domain.dto.MessageDTO;
import com.lyl.ws.service.RemoteMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/message")
@Slf4j
public class ApiMessageController {

    @Autowired
    private RemoteMessageService remoteMessageService;

    /**
     * 根据一致性哈希路由消息
     * 
     * @param messageDTO 消息DTO
     * @return 是否成功
     */
    @PostMapping("/route")
    public boolean routeMessage(@RequestBody MessageDTO messageDTO) {
        log.info("收到消息路由请求: {}", messageDTO);
        return remoteMessageService.routeAndSendMessage(messageDTO);
    }

    /**
     * 接收网关转发的消息
     * 
     * @param messageDTO 消息DTO
     * @return 是否成功
     */
    @PostMapping("/relay")
    public boolean relayMessage(@RequestBody MessageDTO messageDTO) {
        log.info("收到消息转发请求: {}", messageDTO);
        // 使用现有的消息处理逻辑处理转发消息
        return new MessageController().sendMessage(messageDTO);
    }
}
