package com.lyl.ws;

import com.lyl.ws.handler.WebSocketServerInitializer;
import com.lyl.ws.utils.NacosRegisterUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

@Component
@Slf4j
public class NettyServerManager {

    @Resource
    private WebSocketServerInitializer webSocketServerInitializer;

    @Resource
    private NacosRegisterUtil nacosRegisterUtil;

    @Value("${yl_IM.netty.server.port}")
    private int port;

    private final NioEventLoopGroup boss = new NioEventLoopGroup(1);
    private final NioEventLoopGroup worker = new NioEventLoopGroup();
    private final ServerBootstrap bootstrap = new ServerBootstrap();
    private ChannelFuture channelFuture;

    @EventListener(ApplicationReadyEvent.class)
    public void startNettyServer() {
        try {
            log.info("开始启动Netty服务器，端口: {}", port);
            channelFuture = bootstrap.group(boss, worker)
                    .channelFactory(NioServerSocketChannel::new)
                    .childHandler(webSocketServerInitializer)
                    .bind(port)
                    .sync();
            log.info("Netty server started successfully on port {}", port);
            log.info("Netty服务器监听地址: {}", channelFuture.channel().localAddress());
            nacosRegisterUtil.register();
        } catch (InterruptedException e) {
            log.error("Netty server failed to start on port {}: {}", port, e.getMessage());
        }
    }

    @PreDestroy
    public void shutdownNettyServer() {
        nacosRegisterUtil.deregister();
        channelFuture.channel().close();
        worker.shutdownGracefully();
        boss.shutdownGracefully();
        log.info("Netty server on port {} has been shut down", port);
    }
}
