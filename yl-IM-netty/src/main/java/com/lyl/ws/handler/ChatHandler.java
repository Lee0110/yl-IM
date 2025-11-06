package com.lyl.ws.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.lyl.enums.IHandlerKey;
import com.lyl.service.message.IMessageHandler;
import com.lyl.ws.constant.ChannelAttributeKeyConstant;
import com.lyl.service.message.dto.MessageDTO;
import com.lyl.ws.utils.LocalChannelStoreUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Objects;

@ChannelHandler.Sharable
@Component
@Slf4j
public class ChatHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    @Resource
    private LocalChannelStoreUtil localChannelStoreUtil;

    @Resource
    private Map<String, IMessageHandler> messageHandlerMap;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        if (Objects.nonNull(frame)) {
            String msg = frame.text();
            log.info("接收到消息: {}", msg);

            MessageDTO messageDTO;
            try {
                messageDTO = objectMapper.readValue(msg, MessageDTO.class);
            } catch (JsonProcessingException e) {
                log.error("消息JSON解析失败: {}", msg, e);
                return;
            }

            if (Objects.isNull(messageDTO) || Objects.isNull(messageDTO.getType())) {
                log.error("接收到无效消息: {}", msg);
                return;
            }

            IMessageHandler handler = IHandlerKey.getHandler(messageDTO.getType(), messageHandlerMap);
            handler.handleMessage(messageDTO);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("WebSocket连接建立, channel ID: {}", ctx.channel().id());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Long userId = ctx.channel().attr(ChannelAttributeKeyConstant.USER_ID_KEY).get();
        if (userId != null) {
            log.info("用户 {} 断开连接，移除Channel: {}", userId, ctx.channel().id());
            localChannelStoreUtil.removeChannel(userId);
        } else {
            log.warn("Channel断开连接，但无法获取用户ID: {}", ctx.channel().id());
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("WebSocket连接异常: ", cause);
        Long userId = ctx.channel().attr(ChannelAttributeKeyConstant.USER_ID_KEY).get();
        if (userId != null) {
            log.info("用户 {} 异常断开连接，移除Channel: {}", userId, ctx.channel().id());
            localChannelStoreUtil.removeChannel(userId);
        } else {
            log.warn("Channel异常断开连接，但无法获取用户ID: {}", ctx.channel().id());
        }
        ctx.close();
    }
}
