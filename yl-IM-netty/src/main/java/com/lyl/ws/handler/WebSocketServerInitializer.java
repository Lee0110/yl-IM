package com.lyl.ws.handler;

import com.lyl.ws.constant.HandlerNameConstant;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class WebSocketServerInitializer extends ChannelInitializer<SocketChannel> {

    @Resource
    private HeartBeatHandler heartBeatHandler;

    @Resource
    private AuthHandler authHandler;

    @Resource
    private TraceIdMdcHandler traceIdMdcHandler;

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        // websocket基于http协议,需要http编码和解码工具
        pipeline.addLast(HandlerNameConstant.HTTP_CODEC, new HttpServerCodec());
        // 对于大数据流的支持
        pipeline.addLast(HandlerNameConstant.CHUNKED_WRITE, new ChunkedWriteHandler());
        // 聚合器，将多个消息转换为单一的FullHttpRequest或FullHttpResponse
        pipeline.addLast(HandlerNameConstant.HTTP_OBJECT_AGGREGATOR, new HttpObjectAggregator(1024 * 64));

        // 心跳检测处理器
        pipeline.addLast(HandlerNameConstant.IDLE_STATE, new IdleStateHandler(0, 0, 30)); // 30秒无心跳则断开
        pipeline.addLast(HandlerNameConstant.HEART_BEAT_HANDLER, heartBeatHandler);

        // WebSocket协议处理器
        pipeline.addLast(HandlerNameConstant.WEB_SOCKET_PROTOCOL_SERVER,
                new WebSocketServerProtocolHandler("/ws", null, true, 65536, false, true));

        // 在业务逻辑前设置/恢复 MDC traceId
        pipeline.addLast(HandlerNameConstant.TRACE_MDC_HANDLER, traceIdMdcHandler);

        // 认证处理器，必须放在业务处理器之前
        pipeline.addLast(HandlerNameConstant.AUTH_HANDLER, authHandler);
    }
}