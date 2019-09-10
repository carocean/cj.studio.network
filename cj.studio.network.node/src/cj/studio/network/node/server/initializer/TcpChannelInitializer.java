
package cj.studio.network.node.server.initializer;

import cj.studio.network.node.server.handler.TcpChannelHandler;
import cj.studio.util.reactor.IServiceProvider;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;


public class TcpChannelInitializer extends ChannelInitializer<SocketChannel> {
    IServiceProvider parent;
    long heartbeat;

    public TcpChannelInitializer(IServiceProvider parent) {
        this.parent=parent;
        this.heartbeat = (long) parent.getService("$.server.heartbeat");
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new LengthFieldBasedFrameDecoder(81920, 0, 4, 0, 4));
        if (heartbeat > 0) {
            pipeline.addLast(new IdleStateHandler(0, 0, heartbeat, TimeUnit.SECONDS));
        }
        pipeline.addLast(new TcpChannelHandler(parent));
    }

}