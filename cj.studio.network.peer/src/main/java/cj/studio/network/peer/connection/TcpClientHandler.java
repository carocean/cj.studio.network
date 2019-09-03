package cj.studio.network.peer.connection;

import cj.studio.ecm.IServiceProvider;
import cj.studio.network.NetworkFrame;
import cj.studio.network.PackFrame;
import cj.studio.network.peer.INetworkPeer;
import cj.studio.network.peer.INetworkPeerContainer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

class TcpClientHandler extends SimpleChannelInboundHandler<Object> {

    INetworkPeerContainer container;

    public TcpClientHandler(IServiceProvider site) {
        container = (INetworkPeerContainer) site.getService("$.peer.container");
    }

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
        if (frame == null) {
            return;
        }
        String networkName = frame.rootName();
        INetworkPeer peer = container.get(networkName);
        if (peer == null) {
            return;
        }
        frame.url(frame.relativeUrl());
        peer.recieve(frame);
    }
}