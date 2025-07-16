package com.lyl.ws.service;

import com.alibaba.fastjson2.JSONObject;
import com.lyl.domain.dto.MessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class RemoteMessageService {

    private final RestTemplate restTemplate;

    public RemoteMessageService() {
        // 创建带超时配置的RestTemplate
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 连接超时5秒
        factory.setReadTimeout(10000);   // 读取超时10秒
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 通过HTTP调用远程服务发送消息
     *
     * @param serverIpPort 目标服务器ID (IP:端口)
     * @param messageDTO 消息内容
     * @return 是否发送成功
     */
    public boolean sendMessageToRemoteServer(String serverIpPort, MessageDTO messageDTO) {
        try {
            String url = "http://" + serverIpPort + "/message/send";
            log.info("准备发送远程消息，URL: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<MessageDTO> request = new HttpEntity<>(messageDTO, headers);

            Boolean response = restTemplate.postForObject(url, request, Boolean.class);
            log.info("远程消息转发成功，服务器: {}，响应: {}", serverIpPort, response);
            return Boolean.TRUE.equals(response);
        } catch (Exception e) {
            log.error("远程消息转发失败，服务器: {}，错误: {}", serverIpPort, e.getMessage(), e);
            return false;
        }
    }
}
