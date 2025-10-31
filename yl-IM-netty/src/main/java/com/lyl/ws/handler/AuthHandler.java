package com.lyl.ws.handler;

import com.lyl.ws.constant.ChannelAttributeKeyConstant;
import com.lyl.utils.LocalChannelStoreUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@ChannelHandler.Sharable
@Component
@Slf4j
public class AuthHandler extends ChannelInboundHandlerAdapter {

    @Resource
    private LocalChannelStoreUtil localChannelStoreUtil;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            WebSocketServerProtocolHandler.HandshakeComplete handshakeComplete =
                    (WebSocketServerProtocolHandler.HandshakeComplete) evt;

            // 从URI查询参数中获取userId
            String uri = handshakeComplete.requestUri();
            String userIdStr = null;

            // 解析URL参数
            try {
                QueryStringDecoder decoder = new QueryStringDecoder(uri);
                List<String> userIdParams = decoder.parameters().get("userId");
                if (userIdParams != null && !userIdParams.isEmpty()) {
                    userIdStr = userIdParams.get(0);
                }
            } catch (Exception e) {
                log.error("Failed to parse userId from URI: {}", uri, e);
            }

            if (StringUtils.isBlank(userIdStr)) {
                log.warn("Handshake failed: userId parameter is missing from URL");
                ctx.close();
                return;
            }

            try {
                Long userId = Long.parseLong(userIdStr);

                // 测试阶段，userId只要在1-1000000范围内即视为合法
                if (userId < 1 || userId > 1000000) {
                    log.warn("Handshake failed: userId {} is out of valid range", userId);
                    ctx.close();
                    return;
                }

                // 将userId存储到Channel的属性中
                ctx.channel().attr(ChannelAttributeKeyConstant.USER_ID_KEY).set(userId);

                // 将Channel存储到ChannelStore
                localChannelStoreUtil.addChannel(userId, ctx.channel());

                // 认证通过后，移除AuthHandler
                ctx.pipeline().remove(this);

                log.info("用户 {} 连接到服务器，Channel ID: {}", userId, ctx.channel().id());

            } catch (NumberFormatException e) {
                log.warn("Handshake failed: invalid userId format {}", userIdStr, e);
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Exception in AuthHandler: {}", cause.getMessage(), cause);
        ctx.close();
    }
}
