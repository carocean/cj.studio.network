package cj.studio.network.node.server.handler;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.logging.ILogging;
import cj.studio.ecm.net.util.TcpFrameBox;
import cj.studio.network.NetworkFrame;
import cj.studio.network.PackFrame;
import cj.studio.network.node.INetworkContainer;
import cj.studio.network.node.INetworkNodeAppManager;
import cj.studio.util.reactor.Event;
import cj.studio.util.reactor.IReactor;
import cj.studio.util.reactor.IServiceProvider;
import cj.ultimate.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

//使用reactor接收消息
//推送系统仅推送简单本文，对于较大的多媒体文件的推送不在本方案之内，可由客户端直接向文件服务上传而后通过推送系统将地址告诉另一方，另一方自动下载
public class TcpChannelHandler extends ChannelHandlerAdapter {
    public static ILogging logger;
    private long overtimes;
    // 心跳丢失计数器
    private long counter;
    IReactor reactor;
    INetworkContainer container;
    INetworkNodeAppManager appManager;

    public TcpChannelHandler(IServiceProvider parent) {
        logger = CJSystem.logging();
        reactor = (IReactor) parent.getService("$.reactor");
        container = (INetworkContainer) parent.getService("$.network.container");
        appManager = (INetworkNodeAppManager) parent.getService("$.network.app.manager");
        this.overtimes = (long) parent.getService("$.server.overtimes");
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (!(evt instanceof IdleStateEvent)) {
            super.userEventTriggered(ctx, evt);
            return;
        }
        String client = "";
        AttributeKey<String> key = AttributeKey.valueOf("Peer-Name");
        Attribute<String> attribute = ctx.channel().attr(key);
        if (attribute != null && !StringUtil.isEmpty(attribute.get())) {
            client = attribute.get();
        } else {
            client = ctx.channel().remoteAddress().toString();
        }
        // 空闲6s之后触发 (心跳包丢失)
        if (overtimes > 0 && counter >= overtimes) {
            // 连续丢失10个心跳包 (断开连接)
            ctx.channel().close().sync();
            CJSystem.logging().warn(getClass(), String.format("客户端：%s，连续丢失了%s个心跳包 ,服务器主动断开与它的连接.", client, counter));
        } else {
            counter++;
            CJSystem.logging().warn(getClass(), String.format("客户端：%s，已丢失了%s个心跳包.", client, counter));
        }

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //如果心跳则退出，如果是空消息则退出，如果不是frame格式则退出
        //从container中找key，将消息放入reactor
        ByteBuf bb = (ByteBuf) msg;
        if (bb.readableBytes() == 0) {
            return;
        }
        byte[] b = new byte[bb.readableBytes()];
        bb.readBytes(b);
//        bb.release();//系统会释放
        if (b.length < 1) {
            return;
        }
        PackFrame pack = new PackFrame(b);
        if (pack.isInvalid()) {
            return;
        }
        if (pack.isHeartbeat()) {
            counter = 0;
            feedbackHeartbeat(ctx);
//            CJSystem.logging().info(getClass(),"收到心跳包");
            return;
        }
        NetworkFrame frame = pack.getFrame();
        if (frame == null) {
            return;
        }
        String network = frame.rootName();
        if (StringUtil.isEmpty(network)) {
            CJSystem.logging().warn(getClass(), "忽略该上下文为/的请求侦");
            return;
        }
        counter = 0;
        //以下路由到所请求的通道
        String cmd = frame.command();
        Event event = new Event(network, cmd);
        event.getParameters().put("frame", frame);
        event.getParameters().put("channel", ctx.channel());
        reactor.input(event);
    }

    private void feedbackHeartbeat(ChannelHandlerContext ctx) {
        NetworkFrame f = new NetworkFrame("heartbeat / network/1.0");
        PackFrame pack = new PackFrame((byte) 2, f);
        byte[] box = TcpFrameBox.box(pack.toBytes());
        pack.dispose();
        ByteBuf bb = Unpooled.buffer();
        bb.writeBytes(box,0,box.length);
        ctx.channel().writeAndFlush(bb);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().attr(AttributeKey.valueOf("Net-Protocol")).set("tcp");
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        counter = 0;
        container.onChannelInactive(ctx.channel());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        CJSystem.logging().error(getClass(), cause);
    }


}
