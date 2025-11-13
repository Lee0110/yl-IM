package com.lyl.ws.handler;

import com.lyl.utils.MDCContextUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 在 Netty 处理链上为每条入站消息设置 MDC traceId, 并在处理后恢复。
 */
@ChannelHandler.Sharable
@Component
public class TraceIdMdcHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Map<String, String> previous = MDC.getCopyOfContextMap();
        try {
            // 确保每条消息处理时 MDC 中都有 traceId；若不存在则生成一个
            MDCContextUtil.getTraceId();
            super.channelRead(ctx, msg);
        } finally {
            if (previous != null) {
                MDC.setContextMap(previous);
            } else {
                MDC.clear();
            }
        }
    }
}
