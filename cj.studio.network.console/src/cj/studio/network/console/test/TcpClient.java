package cj.studio.network.console.test;

import cj.studio.network.NetworkFrame;
import cj.studio.network.PackFrame;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class TcpClient {
    EventLoopGroup exepool;
    private Channel channel;

    public Sender connect(String ip, int port) {
        EventLoopGroup group = new NioEventLoopGroup(8);
        Bootstrap b = new Bootstrap();

        b.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
                .handler(new TcpClientGatewaySocketInitializer());
        try {
            this.channel = b.connect(ip, port).sync().channel();
            Sender sender=new Sender(channel);
            return sender;
        } catch (Throwable e) {
           throw new RuntimeException(e);
        }
    }

    class TcpClientGatewaySocketInitializer extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast(new LengthFieldBasedFrameDecoder(81920, 0, 4, 0, 4));
//            long interval = (long) parent.getService("$.prop.heartbeat");
//            if (interval > 0) {
//                pipeline.addLast(new IdleStateHandler(0, 0, interval, TimeUnit.MILLISECONDS));
//            }
            pipeline.addLast(new TcpClientHandler());
        }

    }

    class TcpClientHandler extends SimpleChannelInboundHandler<Object> {

        @Override
        protected void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf bb = (ByteBuf) msg;
            if (bb.readableBytes() == 0) {
                return;
            }
            byte[] b = new byte[bb.readableBytes()];
            bb.readBytes(b);
//            bb.release();//系统会释放
            if (b.length < 1) {
                return;
            }
            PackFrame pack = new PackFrame(b);
            if (pack.isInvalid()) {
                return;
            }
            if (pack.isHeartbeat()) {
                return;
            }
            NetworkFrame frame = pack.getFrame();
            if(frame==null){
                return;
            }
            System.out.println("----------------------------------");
            StringBuffer sb=new StringBuffer();
            frame.print(sb);
            System.out.println(sb);

        }
    }
}
