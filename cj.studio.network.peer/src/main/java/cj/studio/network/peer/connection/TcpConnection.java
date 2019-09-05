package cj.studio.network.peer.connection;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.net.util.TcpFrameBox;
import cj.studio.network.NetworkFrame;
import cj.studio.network.PackFrame;
import cj.studio.network.peer.IConnection;
import cj.ultimate.util.StringUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TcpConnection implements IConnection {
    EventLoopGroup exepool;
    private Channel channel;
    IServiceProvider site;
    String peerName;
    private String protocol;
    private String host;
    private int port;
    private long heartbeat;

    public TcpConnection(IServiceProvider site) {
        this.site = site;
        peerName = (String) site.getService("$.peer.name");
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public void connect(String protocol, String ip, int port, Map<String, String> props) {
        this.protocol = protocol;
        this.host = ip;
        this.port = port;
        String strheartbeat = props
                .get("heartbeat");
        if (StringUtil.isEmpty(strheartbeat)) {
            strheartbeat = "0";
        }
        this.heartbeat = Long.valueOf(strheartbeat);

        String workThreadCount = props
                .get("workThreadCount");
        EventLoopGroup group = null;
        if (StringUtil.isEmpty(workThreadCount)) {
            group = new NioEventLoopGroup();
        } else {
            group = new NioEventLoopGroup(Integer.valueOf(workThreadCount));
        }

        Bootstrap b = new Bootstrap();

        b.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
                .handler(new TcpClientGatewaySocketInitializer());
        try {
            this.channel = b.connect(ip, port).sync().channel();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void send(NetworkFrame frame) {
        frame.head("Peer-Name", peerName);
        PackFrame pack = new PackFrame((byte) 1, frame);
        byte[] box = TcpFrameBox.box(pack.toBytes());
        pack.dispose();
        ByteBuf bb = Unpooled.buffer();
        bb.writeBytes(box, 0, box.length);
        channel.writeAndFlush(bb);
    }

    @Override
    public boolean isConnected() {
        return channel.isWritable();
    }

    @Override
    public void close() {
        channel.close();
    }

    class TcpClientGatewaySocketInitializer extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast(new LengthFieldBasedFrameDecoder(81920, 0, 4, 0, 4));
            long interval = heartbeat;
            if (interval > 0) {
                pipeline.addLast(new IdleStateHandler(0, 0, interval, TimeUnit.SECONDS));
            }
            pipeline.addLast(new TcpClientHandler(site));
        }

    }


}
