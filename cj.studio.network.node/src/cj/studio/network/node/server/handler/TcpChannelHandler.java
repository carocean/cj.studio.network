package cj.studio.network.node.server.handler;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.logging.ILogging;
import cj.studio.ecm.net.Circuit;
import cj.studio.network.node.INetworkContainer;
import cj.studio.network.node.ServerInfo;
import cj.studio.util.reactor.Event;
import cj.studio.util.reactor.IReactor;
import cj.studio.util.reactor.Reactor;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;

//使用reactor接收消息
//推送系统仅推送简单本文，对于较大的多媒体文件的推送不在本方案之内，可由客户端直接向文件服务上传而后通过推送系统将地址告诉另一方，另一方自动下载
public class TcpChannelHandler extends ChannelHandlerAdapter {
    public static ILogging logger;
    // 心跳丢失计数器
    private int counter;
    IReactor reactor;

    public TcpChannelHandler(IServiceProvider parent) {
        logger = CJSystem.logging();
        reactor = Reactor.open();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            // 空闲6s之后触发 (心跳包丢失)
            if (counter >= 10) {
                // 连续丢失10个心跳包 (断开连接)
                ctx.channel().close().sync();
            } else {
                counter++;
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //如果心跳则退出，如果是空消息则退出，如果不是frame格式则退出
        ByteBuf bb = (ByteBuf) msg;
        if (bb.readableBytes() == 0) {
            return;
        }
        byte[] b = new byte[bb.readableBytes()];
        bb.readBytes(b);
        bb.release();

        String key="";
        String cmd="";
        Event event = new Event(key,cmd);
        reactor.input(event);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        counter = 0;
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // TODO Auto-generated method stub
        super.exceptionCaught(ctx, cause);
    }
}
