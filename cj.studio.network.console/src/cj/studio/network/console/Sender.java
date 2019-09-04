package cj.studio.network.console;

import cj.studio.ecm.net.util.TcpFrameBox;
import cj.studio.network.NetworkFrame;
import cj.studio.network.PackFrame;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

public class Sender {
    Channel channel;
    public Sender(Channel channel) {
        this.channel=channel;
    }
    public void send(NetworkFrame frame){
        PackFrame pack=new PackFrame((byte)1,frame);
        byte[] box = TcpFrameBox.box(pack.toBytes());
        ByteBuf bb= Unpooled.buffer();
        bb.writeBytes(box,0,box.length);
        channel.writeAndFlush(bb);
    }
}
