package com.lyl.ws.service;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.lyl.domain.dto.MessageDTO;
import com.lyl.ws.utils.ConsistentHashUtil;
import com.lyl.ws.utils.LocalChannelStoreUtil;
import com.lyl.ws.utils.NacosRegisterUtil;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RemoteMessageService {

    private final RestTemplate restTemplate;
    private final NacosDiscoveryProperties nacosDiscoveryProperties;

    @Autowired
    public RemoteMessageService(NacosDiscoveryProperties nacosDiscoveryProperties) {
        this.restTemplate = new RestTemplate();
        this.nacosDiscoveryProperties = nacosDiscoveryProperties;
    }
    
    public RemoteMessageService() {
        this.restTemplate = new RestTemplate();
        this.nacosDiscoveryProperties = null;
    }

    /**
     * 获取所有Netty服务实例
     */
    public List<String> getAllNettyInstances() {
        try {
            NamingService namingService = nacosDiscoveryProperties.namingServiceInstance();
            List<Instance> instances = namingService.getAllInstances(NacosRegisterUtil.YL_IM_NETTY);
            
            return instances.stream()
                    .filter(Instance::isEnabled)
                    .filter(Instance::isHealthy)
                    .map(instance -> instance.getIp() + ":" + instance.getPort())
                    .collect(Collectors.toList());
        } catch (NacosException e) {
            log.error("获取Netty实例列表失败", e);
            return Collections.emptyList();
        }
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
    
    /**
     * 使用一致性哈希找到用户所在服务器并发送消息
     *
     * @param messageDTO 消息对象
     * @return 是否发送成功
     */
    public boolean routeAndSendMessage(MessageDTO messageDTO) {
        Long targetUserId = messageDTO.getReceiverId();
        String targetUserIdStr = String.valueOf(targetUserId);
        
        // 注入LocalChannelStoreUtil
        LocalChannelStoreUtil channelStoreUtil = new LocalChannelStoreUtil();
        
        // 首先检查目标用户是否在本地连接
        Channel localChannel = channelStoreUtil.getChannelByUserId(targetUserId);
        if (localChannel != null && localChannel.isActive()) {
            // 本地发送
            localChannel.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(messageDTO)));
            log.debug("消息在本地发送: {}", messageDTO);
            return true;
        }
        
        // 获取所有Netty实例
        List<String> instances = getAllNettyInstances();
        if (instances.isEmpty()) {
            log.error("没有可用的Netty实例");
            return false;
        }
        
        // 使用一致性哈希算法选择服务器
        String targetServer = ConsistentHashUtil.selectServer(targetUserIdStr, instances);
        if (targetServer == null) {
            log.error("无法确定目标服务器");
            return false;
        }
        
        // 发送到目标服务器
        log.info("使用一致性哈希路由消息，目标用户:{}, 选中服务器:{}", targetUserId, targetServer);
        return sendMessageToRemoteServer(targetServer, messageDTO);
    }
}
