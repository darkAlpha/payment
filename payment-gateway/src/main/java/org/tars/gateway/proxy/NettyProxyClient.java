package org.tars.gateway.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.tars.gateway.context.GatewayContext;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NettyProxyClient {
    private final EventLoopGroup group = new NioEventLoopGroup(4);

    public ProxyResponse forward(GatewayContext ctx, int timeoutMs) {
        try {
            URI uri = new URI(ctx.getResolvedUpstream());
            String host = uri.getHost();
            int port = uri.getPort() == -1 ? ("https".equals(uri.getScheme()) ? 443 : 80) : uri.getPort();
            boolean ssl = "https".equalsIgnoreCase(uri.getScheme());
            String path = uri.getRawPath() + (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "");

            CompletableFuture<ProxyResponse> future = new CompletableFuture<>();
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMs)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override protected void initChannel(SocketChannel ch) throws Exception {
                            if (ssl) {
                                SslContext sslCtx = SslContextBuilder.forClient()
                                        .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
                                ch.pipeline().addLast(sslCtx.newHandler(ch.alloc(), host, port));
                            }
                            ch.pipeline().addLast(new ReadTimeoutHandler(timeoutMs, TimeUnit.MILLISECONDS));
                            ch.pipeline().addLast(new HttpClientCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator(10 * 1024 * 1024));
                            ch.pipeline().addLast(new ResponseHandler(future));
                        }
                    });

            Channel ch = b.connect(host, port).sync().channel();
            HttpMethod method = HttpMethod.valueOf(ctx.getMethod());
            ByteBuf content = ctx.getBody() != null ? Unpooled.wrappedBuffer(ctx.getBody()) : Unpooled.EMPTY_BUFFER;
            FullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, path, content);
            req.headers().set(HttpHeaderNames.HOST, host);
            req.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            req.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
            ctx.getHeaders().forEach((k, v) -> { if (!isHopByHop(k)) req.headers().set(k, v); });
            req.headers().set("X-Forwarded-By", "payment-gateway");
            req.headers().set("X-Request-Id", ctx.getRequestId());
            ch.writeAndFlush(req);

            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("Proxy error: {}", e.getMessage());
            return ProxyResponse.error(502, e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() { group.shutdownGracefully(); }

    private boolean isHopByHop(String h) {
        return h.equalsIgnoreCase("Connection") || h.equalsIgnoreCase("Transfer-Encoding")
                || h.equalsIgnoreCase("Keep-Alive") || h.equalsIgnoreCase("Upgrade");
    }

    private static class ResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
        private final CompletableFuture<ProxyResponse> future;
        ResponseHandler(CompletableFuture<ProxyResponse> future) { this.future = future; }

        @Override protected void channelRead0(ChannelHandlerContext c, FullHttpResponse resp) {
            byte[] body = new byte[resp.content().readableBytes()];
            resp.content().readBytes(body);
            Map<String, String> headers = new HashMap<>();
            resp.headers().forEach(e -> headers.put(e.getKey(), e.getValue()));
            future.complete(new ProxyResponse(resp.status().code(), headers, body));
            c.close();
        }

        @Override public void exceptionCaught(ChannelHandlerContext c, Throwable cause) {
            future.complete(ProxyResponse.error(502, cause.getMessage()));
            c.close();
        }
    }
}
