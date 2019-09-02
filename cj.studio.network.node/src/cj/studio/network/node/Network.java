package cj.studio.network.node;

import cj.studio.ecm.net.util.TcpFrameBox;
import cj.studio.network.Frame;
import cj.studio.network.INetwork;
import cj.studio.network.NetworkInfo;
import cj.studio.network.PackFrame;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Network implements INetwork {
    List<Channel> channels;
    NetworkInfo info;

    public Network(NetworkInfo info) {
        this.info = info;
        channels = new CopyOnWriteArrayList<>();
    }

    @Override
    public void dispose() {
        channels.clear();
        info = null;
    }

    @Override
    public NetworkInfo getInfo() {
        return info;
    }

    @Override
    public void addChannel(Channel ch) {
        channels.add(ch);
    }

    @Override
    public void removeChannel(Channel channel) {
        channels.remove(channel);
    }

    @Override
    public void cast(Channel source, Frame frame) {
        PackFrame pack = new PackFrame((byte) 1, frame);
        byte[] box = TcpFrameBox.box(pack.toBytes());
        ByteBuf bb = Unpooled.buffer();
        bb.writeBytes(box, 0, box.length);
        switch (info.getCastmode()) {
            case "unicast":
                if (channels.isEmpty()) {
                    return;
                }
                Channel one = null;
                boolean found=false;
                int hc=frame.hashCode();
                for (int i = 0; i < channels.size(); i++) {
                    one = channels.get((hc+i) % channels.size());
                    if (one == null) {
                        continue;
                    }
                    if (!one.isWritable()) {
                        one.close();
                        channels.remove(one);
                        continue;
                    }
                    found=true;
                    break;
                }
                if(!found||one==null){
                    return;
                }

                one.writeAndFlush(bb);
                break;
            case "multicast":
                for (Channel ch : channels) {
                    if (ch == null) {
                        continue;
                    }
                    if (!ch.isWritable()) {
                        ch.close();
                        channels.remove(ch);
                    }
                    ch.writeAndFlush(bb);
                }
                break;
            default:
                break;
        }
    }
}
