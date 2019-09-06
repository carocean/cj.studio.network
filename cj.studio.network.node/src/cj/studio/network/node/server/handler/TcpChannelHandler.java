package cj.studio.network.node.server.handler;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.logging.ILogging;
import cj.studio.network.NetworkCircuit;
import cj.studio.network.NetworkFrame;
import cj.studio.network.PackFrame;
import cj.studio.network.node.INetworkContainer;
import cj.studio.network.node.INetworkNodeAppManager;
import cj.studio.util.reactor.*;
import cj.ultimate.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

//使用reactor接收消息
//推送系统仅推送简单本文，对于较大的多媒体文件的推送不在本方案之内，可由客户端直接向文件服务上传而后通过推送系统将地址告诉另一方，另一方自动下载
public class TcpChannelHandler extends ChannelHandlerAdapter {
    public static ILogging logger;
    // 心跳丢失计数器
    private int counter;
    IReactor reactor;
    INetworkContainer container;
    INetworkNodeAppManager appManager;

    public TcpChannelHandler(IServiceProvider parent) {
        logger = CJSystem.logging();
        reactor = (IReactor) parent.getService("$.reactor");
        container = (INetworkContainer) parent.getService("$.network.container");
        appManager = (INetworkNodeAppManager) parent.getService("$.network.app.manager");
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            String client="";
            AttributeKey<String> key=AttributeKey.valueOf("Peer-Name");
            Attribute<String> attribute=ctx.channel().attr(key);
            if(attribute!=null&&!StringUtil.isEmpty(attribute.get())){
                client=attribute.get();
            }else{
                client=ctx.channel().remoteAddress().toString();
            }
            // 空闲6s之后触发 (心跳包丢失)
            if (counter >= 10) {
                // 连续丢失10个心跳包 (断开连接)
                ctx.channel().close().sync();
                CJSystem.logging().warn(getClass(),String.format("客户端：%s，连续丢失了%s个心跳包 ,服务器主动断开与它的连接.",client,counter));
            } else {
                counter++;
                CJSystem.logging().warn(getClass(),String.format("客户端：%s，已丢失了%s个心跳包.",client,counter));
            }
        } else {
            super.userEventTriggered(ctx, evt);
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
            counter=0;
            return;
        }
        NetworkFrame frame = pack.getFrame();
        if (frame == null) {
            return;
        }
        String network = frame.rootName();
        if (StringUtil.isEmpty(network)) {
            return;
        }
        counter=0;
        if (!container.existsNetwork(network)) {//如果不存在请求的网络则走管理网络，管理网络负责将这错误发给侦听的客户端
            //发给管理命令
            ByteBuf data = Unpooled.buffer();
            NetworkFrame f = new NetworkFrame(String.format("error /%s network/1.0", container.getMasterNetworkName()), data);
            NetworkCircuit c = new NetworkCircuit(String.format("network/1.0 404 The Network %s is Not Exists.", network));
            c.content().writeBytes(frame.toByteBuf());
            data.writeBytes(c.toByteBuf());
            Event event = new Event(container.getMasterNetworkName(), f.command());
            event.getParameters().put("frame", f);
            event.getParameters().put("channel", ctx.channel());
            reactor.input(event);
            return;
        }
        //以下路由到所请求的通道
        String cmd = frame.command();
        Event event = new Event(network, cmd);
        event.getParameters().put("frame", frame);
        event.getParameters().put("channel", ctx.channel());
        reactor.input(event);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
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
        cause.printStackTrace();
        String network = container.getMasterNetworkName();
        NetworkFrame frame = new NetworkFrame(String.format("error %s network/1.0", network));
        Event event = new Event(network, frame.command());
        event.getParameters().put("frame", frame);
        event.getParameters().put("channel", ctx.channel());
        reactor.input(event);

    }


}
