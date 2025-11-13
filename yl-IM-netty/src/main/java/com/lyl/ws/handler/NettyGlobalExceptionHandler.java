package com.lyl.ws.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;

@Slf4j
@ChannelHandler.Sharable
@Component
public class NettyGlobalExceptionHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof ClosedChannelException) {
            log.debug("通道已关闭, 忽略异常: {}", cause.toString());
            return;
        }
        if (cause instanceof IOException) {
            String msg = cause.getMessage();
            if (msg != null && (msg.contains("Connection reset") || msg.contains("Broken pipe"))) {
                log.debug("网络断开/对端复位, 忽略异常: {}", msg);
                return;
            }
        }
        log.warn("捕获未处理异常, 关闭通道。channelId={}", ctx.channel().id().asShortText(), cause);
        ctx.close();
    }
}
