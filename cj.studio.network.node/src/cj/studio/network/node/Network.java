package cj.studio.network.node;

import cj.studio.ecm.net.util.TcpFrameBox;
import cj.studio.network.NetworkFrame;
import cj.studio.network.PackFrame;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.util.ArrayList;
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
    public String[] enumPeerName() {
        List<String> list = new ArrayList<>();
        for (Channel ch : channels) {
            if (ch == null) continue;
            Attribute<String> attr = ch.attr(AttributeKey.valueOf("Peer-Name"));
            if (attr == null) continue;
            String name = attr.get();
            list.add(name);
        }
        return list.toArray(new String[0]);
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
    public void cast(Channel source, NetworkFrame frame) {
        PackFrame pack = new PackFrame((byte) 1, frame);
        byte[] box = TcpFrameBox.box(pack.toBytes());
        pack.dispose();
        ByteBuf bb = Unpooled.buffer();
        bb.writeBytes(box, 0, box.length);
        switch (info.getCastmode()) {
            case "unicast":
                unicast(frame.hashCode(), bb);
                break;
            case "multicast":
                multicast(bb);
                break;
            case "feedbackcast":
                feedbackcast(source, bb);
                break;
            default:
                break;
        }
    }

    private void feedbackcast(Channel source, ByteBuf bb) {
        source.writeAndFlush(bb);
    }

    @Override
    public boolean existsChannel(Channel channel) {
        return this.channels.contains(channel);
    }

    private void multicast(ByteBuf bb) {
        for (Channel ch : channels) {
            if (ch == null) {
                continue;
            }
            if (!ch.isWritable()) {
                ch.close();
                channels.remove(ch);
            }
            ByteBuf copy = bb.copy();//多播必须使用拷贝
            ch.writeAndFlush(copy);
        }
        bb.release();//把源形释放，这个与单播不同，因为它有原型
    }

    private void unicast(int hc, ByteBuf bb) {
        if (channels.isEmpty()) {
            return;
        }
        Channel one = null;
        boolean found = false;
        for (int i = 0; i < channels.size(); i++) {
            one = channels.get((hc + i) % channels.size());
            if (one == null) {
                continue;
            }
            if (!one.isWritable()) {
                one.close();
                channels.remove(one);
                continue;
            }
            found = true;
            break;
        }
        if (!found || one == null) {
            return;
        }

        one.writeAndFlush(bb);
    }
}
