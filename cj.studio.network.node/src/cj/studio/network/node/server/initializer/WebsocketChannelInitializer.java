
package cj.studio.network.node.server.initializer;

import cj.studio.network.node.server.handler.WebsocketChannelHandler;
import cj.studio.util.reactor.IServiceProvider;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;


public class WebsocketChannelInitializer extends ChannelInitializer<SocketChannel> {
    IServiceProvider parent;
    long heartbeat;
    int maxContentLength;
    String wspath;
    public WebsocketChannelInitializer(IServiceProvider parent) {
        this.parent=parent;
        this.heartbeat = (long) parent.getService("$.server.heartbeat");
        this.maxContentLength = (int) parent.getService("$.server.maxContentLength");
        this.wspath = (String) parent.getService("$.server.wspath");
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new HttpRequestDecoder());
        pipeline.addLast(new HttpObjectAggregator(maxContentLength));
//		 pipeline.addLast(new WebSocketFrameAggregator(aggregator));
        pipeline.addLast(new HttpResponseEncoder());
        pipeline.addLast(new WebSocketServerProtocolHandler(wspath));
        if (heartbeat > 0) {
            pipeline.addLast(new IdleStateHandler(heartbeat, 0, 0, TimeUnit.SECONDS));
        }
        pipeline.addLast(new WebsocketChannelHandler(parent));
    }

}