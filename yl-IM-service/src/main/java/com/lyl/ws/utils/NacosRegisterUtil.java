package com.lyl.ws.utils;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.lyl.utils.ConsistentHashUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

@Slf4j
@Component
public class NacosRegisterUtil {
    @Value("${yl_IM.netty.server.port}")
    private int nettyPort;

    @Value("${yl_IM.netty.server.name}")
    private String nettyServerName;

    @Resource
    private NamingService namingService;

    public void register() {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            Instance instance = new Instance();
            instance.setIp(ip);
            instance.setPort(nettyPort);
            namingService.registerInstance(nettyServerName, instance);
            log.info("[Nacos] 注册服务: {} {}:{}", nettyServerName, ip, nettyPort);
        } catch (NacosException | UnknownHostException e) {
            log.error("[Nacos] 注册服务失败: {}", e.getMessage());
        }
    }

    public void deregister() {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            namingService.deregisterInstance(nettyServerName, ip, nettyPort);
            log.info("[Nacos] 注销服务: {} {}:{}", nettyServerName, ip, nettyPort);
        } catch (NacosException | UnknownHostException e) {
            log.error("[Nacos] 注销服务失败: {}", e.getMessage());
        }
    }
}
