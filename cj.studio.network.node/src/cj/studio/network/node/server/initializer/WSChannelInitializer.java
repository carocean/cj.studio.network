
package cj.studio.network.node.server.initializer;

import cj.studio.network.node.server.handler.WSChannelHandler;
import cj.studio.util.reactor.IServiceProvider;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class WSChannelInitializer extends ChannelInitializer<SocketChannel> {
    IServiceProvider parent;
    long heartbeat;
    int maxContentLength;
    String wspath;
    SslContext sslCtx;

    public WSChannelInitializer(boolean SSL,IServiceProvider parent) throws CertificateException, SSLException {
        this.parent = parent;
        this.heartbeat = (long) parent.getService("$.server.heartbeat");
        this.maxContentLength = (int) parent.getService("$.server.maxContentLength");
        this.wspath = (String) parent.getService("$.server.wspath");

        if (SSL) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContext.newServerContext(ssc.certificate(), ssc.privateKey());
        } else {
            sslCtx = null;
        }
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        if (sslCtx != null) {
            pipeline.addLast(sslCtx.newHandler(ch.alloc()));
        }

        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(maxContentLength));
//		 pipeline.addLast(new WebSocketFrameAggregator(aggregator));
        ch.pipeline().addLast(new ChunkedWriteHandler());
        pipeline.addLast(new MyWebSocketProtocolHandler(wspath));
        if (heartbeat > 0) {
            pipeline.addLast(new IdleStateHandler(0, 0, heartbeat, TimeUnit.SECONDS));
        }
        pipeline.addLast(new WSChannelHandler(parent));
    }
class MyWebSocketProtocolHandler extends WebSocketServerProtocolHandler{
    public MyWebSocketProtocolHandler(String websocketPath) {
        super(websocketPath);
    }

    public MyWebSocketProtocolHandler(String websocketPath, String subprotocols) {
        super(websocketPath, subprotocols);
    }

    public MyWebSocketProtocolHandler(String websocketPath, String subprotocols, boolean allowExtensions) {
        super(websocketPath, subprotocols, allowExtensions);
    }

    public MyWebSocketProtocolHandler(String websocketPath, String subprotocols, boolean allowExtensions, int maxFrameSize) {
        super(websocketPath, subprotocols, allowExtensions, maxFrameSize);
    }

    public MyWebSocketProtocolHandler(String websocketPath, String subprotocols, boolean allowExtensions, int maxFrameSize, boolean allowMaskMismatch) {
        super(websocketPath, subprotocols, allowExtensions, maxFrameSize, allowMaskMismatch);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> out) throws Exception {
        out.add(frame.retain());//让心跳包发过去
    }
}
}