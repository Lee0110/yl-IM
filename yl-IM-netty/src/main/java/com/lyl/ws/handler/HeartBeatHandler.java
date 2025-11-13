package com.lyl.ws.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyl.enums.MessageTypeEnum;
import com.lyl.service.message.dto.MessageDTO;
import com.lyl.ws.constant.ChannelAttributeKeyConstant;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@ChannelHandler.Sharable
@Component
public class HeartBeatHandler extends ChannelInboundHandlerAdapter {
    private static final String PING = "ping";
    private static final String PONG = "pong";

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().attr(ChannelAttributeKeyConstant.HEART_BEAT_TIMES).set(new AtomicInteger(0));
        log.debug("初始化心跳计数器, channelId={}", ctx.channel().id().asShortText());
        super.handlerAdded(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof TextWebSocketFrame) {
            String data = ((TextWebSocketFrame) msg).text();
            if (PING.equals(data)) {
                try {
                    if (!ctx.channel().isActive() || !ctx.channel().isOpen()) {
                        return;
                    }
                    // 回复pong
                    ctx.writeAndFlush(new TextWebSocketFrame(PONG));

                    // 递增心跳次数
                    AtomicInteger times = ctx.channel().attr(ChannelAttributeKeyConstant.HEART_BEAT_TIMES).get();
                    int n = times.incrementAndGet();

                    if (n % 10 == 0) {
                        log.debug("channelId={}, 心跳已达{}次", ctx.channel().id().asShortText(), n);
                        // 可以触发一些事件ctx.fireUserEventTriggered
                    }
                } finally {
                    // 释放消息,防止内存泄漏
                    ReferenceCountUtil.release(msg);
                }
                // ping消息已处理,不再向下传递
                return;
            }
        }
        // 其他消息继续传递给下一个Handler
        super.channelRead(ctx, msg);
    }

    /**
     * 处理空闲超时事件<p>
     * 由IdleStateHandler触发,当连接长时间无活动时关闭连接
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.ALL_IDLE) {
                log.debug("连接长时间无活动, 关闭连接, channelId={}", ctx.channel().id().asShortText());
                if (!ctx.channel().isActive() || !ctx.channel().isOpen()) {
                    ctx.close();
                    return;
                }
                MessageDTO messageDTO = new MessageDTO();
                messageDTO.setType(MessageTypeEnum.SYSTEM);
                messageDTO.setSenderId(-1L);
                messageDTO.setReceiverId(ctx.channel().attr(ChannelAttributeKeyConstant.USER_ID_KEY).get());
                messageDTO.setContent("长时间无响应, 已自动关闭");
                try {
                    ctx.channel().writeAndFlush(new TextWebSocketFrame(objectMapper.writeValueAsString(messageDTO)));
                } catch (Exception e) {
                    log.debug("关闭前写入提示失败(可能已关闭), channelId={}", ctx.channel().id().asShortText());
                }
                ctx.channel().close();
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        AtomicInteger times = ctx.channel().attr(ChannelAttributeKeyConstant.HEART_BEAT_TIMES).get();
        if (Objects.nonNull(times)) {
            log.debug("移除心跳计数器, channelId={}, 总心跳次数={}", ctx.channel().id().asShortText(), times.get());
        }
        super.handlerRemoved(ctx);
    }
}