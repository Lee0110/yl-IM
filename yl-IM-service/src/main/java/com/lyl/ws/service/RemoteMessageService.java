package com.lyl.ws.service;

import com.alibaba.fastjson2.JSONObject;
import com.lyl.domain.dto.MessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class RemoteMessageService {

    private final RestTemplate restTemplate;

    public RemoteMessageService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 通过HTTP调用远程服务发送消息
     *
     * @param serverId 目标服务器ID (IP:端口)
     * @param messageDTO 消息内容
     * @return 是否发送成功
     */
    public boolean sendMessageToRemoteServer(String serverId, MessageDTO messageDTO) {
        try {
            String url = "http://" + serverId + "/message/send";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(JSONObject.toJSONString(messageDTO), headers);

            String response = restTemplate.postForObject(url, request, String.class);
            log.info("远程消息转发成功，服务器: {}，响应: {}", serverId, response);
            return true;
        } catch (Exception e) {
            log.error("远程消息转发失败，服务器: {}，错误: {}", serverId, e.getMessage());
            return false;
        }
    }
}
