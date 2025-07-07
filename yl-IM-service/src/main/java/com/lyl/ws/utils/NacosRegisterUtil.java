package com.lyl.ws.utils;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

@Slf4j
@Component
public class NacosRegisterUtil {
    @Value("${spring.cloud.nacos.discovery.server-addr}")
    private String nacosAddr;

    @Value("${server.port}")
    private int serverPort;

    private NamingService namingService;

    public static String SERVER_IP_PORT;

    public static final String YL_IM_NETTY = "yl-im-netty";

    @PostConstruct
    public void init() throws NacosException {
        Properties properties = new Properties();
        properties.setProperty("serverAddr", nacosAddr);
        namingService = NacosFactory.createNamingService(properties);
    }

    public void register() {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            Instance instance = new Instance();
            instance.setIp(ip);
            instance.setPort(serverPort);
            SERVER_IP_PORT = ip + ":" + serverPort;
            namingService.registerInstance(YL_IM_NETTY, instance);
            log.info("[Nacos] 注册服务: {} {}:{}", YL_IM_NETTY, ip, serverPort);
        } catch (NacosException | UnknownHostException e) {
            log.error("[Nacos] 注册服务失败: {}", e.getMessage());
        }
    }

    public void deregister() {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            namingService.deregisterInstance(YL_IM_NETTY, ip, serverPort);
            log.info("[Nacos] 注销服务: {} {}:{}", YL_IM_NETTY, ip, serverPort);
        } catch (NacosException | UnknownHostException e) {
            log.error("[Nacos] 注销服务失败: {}", e.getMessage());
        }
    }
}

