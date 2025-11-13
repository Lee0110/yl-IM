package com.lyl.ws.handler;

import com.lyl.ws.constant.ChannelAttributeKeyConstant;
import com.lyl.ws.constant.HandlerNameConstant;
import com.lyl.ws.utils.LocalChannelStoreUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 自定义的 WebSocketServerProtocolHandler：
 * - 在握手阶段先做 userId 校验, 失败直接返回 401/403, 不发送 101 升级
 * - 通过后再执行标准的 Netty 握手流程, 并主动触发握手完成事件
 */
@ChannelHandler.Sharable
@Component
@Slf4j
public class CustomWebSocketServerProtocolHandler extends WebSocketServerProtocolHandler {

    private final WebSocketServerProtocolConfig serverConfig;

    @Resource
    private LocalChannelStoreUtil localChannelStoreUtil;

    @Resource
    private ChatHandler chatHandler;

    public CustomWebSocketServerProtocolHandler() {
        this(WebSocketServerProtocolConfig.newBuilder()
                .websocketPath("/ws")
                .subprotocols(null)
                .checkStartsWith(true)
                .handshakeTimeoutMillis(10_000)
                .decoderConfig(WebSocketDecoderConfig.newBuilder().maxFramePayloadLength(65536).build())
                .build());
    }

    public CustomWebSocketServerProtocolHandler(WebSocketServerProtocolConfig serverConfig) {
        super(serverConfig);
        this.serverConfig = serverConfig;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        ChannelPipeline cp = ctx.pipeline();
        // 在当前 handler 之前插入自定义握手处理器
        cp.addBefore(ctx.name(), CustomHandshakeHandler.class.getName(), new CustomHandshakeHandler(serverConfig));

        // 如有需要, 添加 UTF8 校验（与 Netty 行为保持一致）
        if (serverConfig.decoderConfig().withUTF8Validator() && cp.get(Utf8FrameValidator.class) == null) {
            cp.addBefore(ctx.name(), Utf8FrameValidator.class.getName(),
                    new Utf8FrameValidator(serverConfig.decoderConfig().closeOnProtocolViolation()));
        }
        // 注意：不调用 super.handlerAdded(ctx), 避免默认的握手处理器被加入
    }

    private final class CustomHandshakeHandler extends ChannelInboundHandlerAdapter {
        private final WebSocketServerProtocolConfig cfg;
        private ChannelPromise handshakePromise;
        private boolean isWebSocketPath;

        private CustomHandshakeHandler(WebSocketServerProtocolConfig cfg) {
            this.cfg = cfg;
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            handshakePromise = ctx.newPromise();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            final HttpObject httpObject = (HttpObject) msg;

            if (httpObject instanceof HttpRequest) {
                final HttpRequest req = (HttpRequest) httpObject;
                isWebSocketPath = isWebSocketPath(req);
                if (!isWebSocketPath) {
                    ctx.fireChannelRead(msg);
                    return;
                }

                try {
                    // 先做鉴权（从查询参数读取 userId）
                    QueryStringDecoder decoder = new QueryStringDecoder(req.uri());
                    String userIdStr = null;
                    List<String> userIdParams = decoder.parameters().get("userId");
                    if (userIdParams != null && !userIdParams.isEmpty()) {
                        userIdStr = userIdParams.get(0);
                    }

                    if (StringUtils.isBlank(userIdStr)) {
                        reject(ctx, HttpResponseStatus.UNAUTHORIZED, "Missing userId parameter");
                        return;
                    }

                    Long userId;
                    try {
                        userId = Long.parseLong(userIdStr);
                    } catch (NumberFormatException e) {
                        log.warn("Invalid userId format: {}", userIdStr, e);
                        reject(ctx, HttpResponseStatus.UNAUTHORIZED, "Invalid userId format");
                        return;
                    }

                    if (userId < 1 || userId > 1_000_000) {
                        log.warn("Handshake denied: userId {} out of range", userId);
                        reject(ctx, HttpResponseStatus.FORBIDDEN, "userId out of valid range");
                        return;
                    }

                    // 鉴权通过：写入属性并记录 channel
                    ctx.channel().attr(ChannelAttributeKeyConstant.USER_ID_KEY).set(userId);
                    localChannelStoreUtil.addChannel(userId, ctx.channel());

                    // 构建 Handshaker 并执行握手
                    WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                            getWebSocketLocation(ctx.pipeline(), req, cfg.websocketPath()),
                            cfg.subprotocols(), cfg.decoderConfig());
                    WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);

                    if (handshaker == null) {
                        WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
                    } else {
                        // 不调用包可见的 setHandshaker, 直接移除自定义握手处理器并继续
                        ctx.pipeline().remove(this);

                        ChannelFuture handshakeFuture = handshaker.handshake(ctx.channel(), req);
                        final ChannelPromise localPromise = handshakePromise;
                        handshakeFuture.addListener((ChannelFutureListener) future -> {
                            if (!future.isSuccess()) {
                                localPromise.tryFailure(future.cause());
                                ctx.fireExceptionCaught(future.cause());
                            } else {
                                localPromise.trySuccess();
                                // 先接入业务处理器（在全局异常处理器之前）, 再派发握手完成事件
                                if (ctx.pipeline().get(ChatHandler.class) == null) {
                                    if (ctx.pipeline().get(HandlerNameConstant.GLOBAL_EXCEPTION_HANDLER) != null) {
                                        ctx.pipeline().addBefore(HandlerNameConstant.GLOBAL_EXCEPTION_HANDLER,
                                                HandlerNameConstant.CHAT_HANDLER, chatHandler);
                                    } else {
                                        ctx.pipeline().addLast(HandlerNameConstant.CHAT_HANDLER, chatHandler);
                                    }
                                }
                                ctx.fireUserEventTriggered(new HandshakeComplete(
                                        req.uri(), req.headers(), handshaker.selectedSubprotocol()));
                            }
                        });
                        applyHandshakeTimeout(ctx, localPromise, cfg.handshakeTimeoutMillis());
                    }
                } finally {
                    ReferenceCountUtil.release(req);
                }
            } else if (!isWebSocketPath) {
                ctx.fireChannelRead(msg);
            } else {
                ReferenceCountUtil.release(msg);
            }
        }

        private boolean isWebSocketPath(HttpRequest req) {
            String websocketPath = cfg.websocketPath();
            String uri = req.uri();
            boolean starts = uri.startsWith(websocketPath);
            boolean okNext = "/".equals(websocketPath) || hasValidNext(uri, websocketPath);
            return cfg.checkStartsWith() ? (starts && okNext) : uri.equals(websocketPath);
        }

        private boolean hasValidNext(String uri, String websocketPath) {
            int len = websocketPath.length();
            if (uri.length() > len) {
                char next = uri.charAt(len);
                return next == '/' || next == '?';
            }
            return true;
        }

        private void applyHandshakeTimeout(ChannelHandlerContext ctx, ChannelPromise promise, long timeoutMillis) {
            if (timeoutMillis <= 0 || promise.isDone()) return;
            ctx.executor().schedule(() -> {
                if (!promise.isDone() && promise.tryFailure(new WebSocketServerHandshakeException("handshake timed out"))) {
                    ctx.flush().fireUserEventTriggered(ServerHandshakeStateEvent.HANDSHAKE_TIMEOUT).close();
                }
            }, timeoutMillis, TimeUnit.MILLISECONDS);
        }

        private String getWebSocketLocation(ChannelPipeline cp, HttpRequest req, String path) {
            boolean ssl = cp.get(SslHandler.class) != null;
            String scheme = ssl ? "wss" : "ws";
            String host = req.headers().get(HttpHeaderNames.HOST);
            return scheme + "://" + host + path;
        }

        private void reject(ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer(message, CharsetUtil.UTF_8));
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
            response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
