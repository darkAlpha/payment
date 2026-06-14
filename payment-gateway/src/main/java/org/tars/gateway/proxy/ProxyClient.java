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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tars.gateway.context.GatewayContext;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Netty-based HTTP proxy client.
 * Forwards requests to upstream services and collects responses.
 */
public class ProxyClient {

    private static final Logger log = LoggerFactory.getLogger(ProxyClient.class);

    private final EventLoopGroup workerGroup;
    private final int defaultTimeoutMs;

    public ProxyClient(int defaultTimeoutMs) {
        this.workerGroup = new NioEventLoopGroup(4);
        this.defaultTimeoutMs = defaultTimeoutMs;
    }

    /**
     * Forward request to upstream and return response.
     */
    public ProxyResponse forward(GatewayContext context, int timeoutMs) {
        String targetUrl = context.getResolvedUpstream();
        if (targetUrl == null || targetUrl.isBlank()) {
            return ProxyResponse.error(502, "No upstream resolved");
        }

        try {
            URI uri = new URI(targetUrl);
            String host = uri.getHost();
            int port = uri.getPort();
            boolean ssl = "https".equalsIgnoreCase(uri.getScheme());

            if (port == -1) {
                port = ssl ? 443 : 80;
            }

            String path = uri.getRawPath();
            if (uri.getRawQuery() != null) {
                path += "?" + uri.getRawQuery();
            }

            CompletableFuture<ProxyResponse> responseFuture = new CompletableFuture<>();
            int effectiveTimeout = timeoutMs > 0 ? timeoutMs : defaultTimeoutMs;

            Bootstrap bootstrap = new Bootstrap();
            final int finalPort = port;
            bootstrap.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, effectiveTimeout)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            if (ssl) {
                                SslContext sslCtx = SslContextBuilder.forClient()
                                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                        .build();
                                pipeline.addLast(sslCtx.newHandler(ch.alloc(), host, finalPort));
                            }
                            pipeline.addLast(new ReadTimeoutHandler(effectiveTimeout, TimeUnit.MILLISECONDS));
                            pipeline.addLast(new HttpClientCodec());
                            pipeline.addLast(new HttpObjectAggregator(10 * 1024 * 1024));
                            pipeline.addLast(new ProxyResponseHandler(responseFuture));
                        }
                    });

            ChannelFuture connectFuture = bootstrap.connect(host, port).sync();

            // Build the outgoing request
            HttpMethod method = HttpMethod.valueOf(context.getMethod());
            ByteBuf content = context.getBody() != null 
                    ? Unpooled.wrappedBuffer(context.getBody()) 
                    : Unpooled.EMPTY_BUFFER;

            FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, path, content);
            request.headers().set(HttpHeaderNames.HOST, host);
            request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            request.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());

            // Forward original headers
            for (Map.Entry<String, String> header : context.getHeaders().entrySet()) {
                if (!isHopByHopHeader(header.getKey())) {
                    request.headers().set(header.getKey(), header.getValue());
                }
            }

            // Add gateway identification
            request.headers().set("X-Forwarded-By", "payment-gateway");
            request.headers().set("X-Request-Id", context.getRequestId());

            connectFuture.channel().writeAndFlush(request);

            ProxyResponse response = responseFuture.get(effectiveTimeout, TimeUnit.MILLISECONDS);
            log.debug("Proxy response from {}: status={}", targetUrl, response.statusCode());
            return response;

        } catch (Exception e) {
            log.error("Proxy error for {}: {}", targetUrl, e.getMessage());
            return ProxyResponse.error(502, "Upstream error: " + e.getMessage());
        }
    }

    private boolean isHopByHopHeader(String header) {
        return header.equalsIgnoreCase("Connection") ||
               header.equalsIgnoreCase("Keep-Alive") ||
               header.equalsIgnoreCase("Transfer-Encoding") ||
               header.equalsIgnoreCase("TE") ||
               header.equalsIgnoreCase("Trailer") ||
               header.equalsIgnoreCase("Upgrade") ||
               header.equalsIgnoreCase("Proxy-Authorization") ||
               header.equalsIgnoreCase("Proxy-Authenticate");
    }

    public void shutdown() {
        workerGroup.shutdownGracefully();
    }

    /**
     * Handler to collect the upstream response.
     */
    private static class ProxyResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

        private final CompletableFuture<ProxyResponse> future;

        ProxyResponseHandler(CompletableFuture<ProxyResponse> future) {
            this.future = future;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) {
            int status = response.status().code();
            byte[] body = new byte[response.content().readableBytes()];
            response.content().readBytes(body);

            Map<String, String> headers = new java.util.HashMap<>();
            response.headers().forEach(entry -> headers.put(entry.getKey(), entry.getValue()));

            future.complete(new ProxyResponse(status, headers, body));
            ctx.close();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            future.complete(ProxyResponse.error(502, cause.getMessage()));
            ctx.close();
        }
    }
}

