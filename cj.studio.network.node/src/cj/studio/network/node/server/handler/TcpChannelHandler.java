package cj.studio.network.node.server.handler;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.logging.ILogging;
import cj.studio.network.Frame;
import cj.studio.network.PackFrame;
import cj.studio.network.node.VerifyException;
import cj.studio.network.node.INetworkContainer;
import cj.studio.network.node.INetworkNodeApp;
import cj.studio.util.reactor.*;
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
    INetworkContainer container;
    INetworkNodeApp app;

    public TcpChannelHandler(IServiceProvider parent) {
        logger = CJSystem.logging();
        reactor = (IReactor) parent.getService("$.reactor");
        container = (INetworkContainer) parent.getService("$.network.container");
        app = (INetworkNodeApp) parent.getService("$.network.app");
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
        //从container中找key，将消息放入reactor
        ByteBuf bb = (ByteBuf) msg;
        if (bb.readableBytes() == 0) {
            return;
        }
        byte[] b = new byte[bb.readableBytes()];
        bb.readBytes(b);
        bb.release();
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
        Frame frame = pack.getFrame();
        //这中间添加第三方验证frame的令牌的逻辑，如果需要的话。验证的逻辑从节点应用中调取
        try {
            app.verifyFrame(frame);
        } catch (Throwable e) {//验证不通过将错误信息发给管理网络，并由管理网络决定是否关闭该channel，
            String cmd = "doNotVerifyFrame";
            String network = container.getManagerNetworkInfo().getName();
            Event event = new Event(network, cmd);
            event.getParameters().put("frame", frame);
            event.getParameters().put("channel", ctx.channel());
            reactor.input(event);
            return;
        }
        String network = frame.rootName();
        if (!container.existsNetwork(network)) {//如果不存在请求的网络则走管理网络，管理网络负责将这错误发给侦听的客户端
            String cmd = "doNotExistsNetwork";
            //发给管理网络
            network = container.getManagerNetworkInfo().getName();
            Event event = new Event(network, cmd);
            event.getParameters().put("frame", frame);
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
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // TODO Auto-generated method stub
        super.exceptionCaught(ctx, cause);
    }
}
