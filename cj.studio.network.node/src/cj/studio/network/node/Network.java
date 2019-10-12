package cj.studio.network.node;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.net.util.TcpFrameBox;
import cj.studio.network.*;
import cj.ultimate.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Network implements INetwork {
    //    List<Channel> channels;
    Map<String, Channel> channels;//key:peer
    Map<String, String> userIndex;//key:user,value:peer
    IPeerEvent peerEvent;
    NetworkConfig config;

    public Network(NetworkConfig config, IPeerEvent peerEvent) {
        this.config = config;
        this.peerEvent = peerEvent;
//        channels = new CopyOnWriteArrayList<>();
        channels = new ConcurrentHashMap<>();
        userIndex = new ConcurrentHashMap<>();
    }

    @Override
    public INetwork createReference() {
        Network network = new Network(config, peerEvent);
        network.channels = channels;
        network.userIndex = userIndex;
        return network;
    }

    @Override
    public void rename(String newNetworkName) {
        config.setName(newNetworkName);
    }

    @Override
    public void changeCastmode(String castmode) {
        config.setCastmode(castmode);
    }

    @Override
    public String[] enumPeerName() {
        List<String> list = new ArrayList<>();
        for (Channel ch : channels.values()) {
            if (ch == null) continue;
            AttributeKey<String> key = AttributeKey.valueOf("Peer-Name");
            String name = ch.attr(key).get();
            list.add(name);
        }
        return list.toArray(new String[0]);
    }

    @Override
    public void dispose() {
        channels.clear();
    }

    @Override
    public NetworkInfo getInfo() {
        return NetworkInfo.parse(config, channels);
    }

    @Override
    public void addChannel(Channel ch) {
        AttributeKey<String> pnkey = AttributeKey.valueOf("Peer-Name");

        Attribute<String> pnattribute = ch.attr(pnkey);
        if (pnattribute == null) {
            return;
        }
        String peerName = pnattribute.get();
        if (StringUtil.isEmpty(peerName)) {
            return;
        }
        synchronized (peerName) {
            if (channels.containsKey(peerName)) {
                return;
            }
            channels.put(peerName, ch);
            AttributeKey<UserPrincipal> upKey = AttributeKey.valueOf("Peer-UserPrincipal");
            UserPrincipal userPrincipal = ch.attr(upKey).get();
            if (userPrincipal != null) {
                userIndex.put(userPrincipal.getName(), peerName);
            }
            if (peerEvent != null) {
                try {
                    peerEvent.online(peerName, userPrincipal, ch, this);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void removeChannel(Channel ch) {
        AttributeKey<String> pnkey = AttributeKey.valueOf("Peer-Name");
        Attribute<String> pnattribute = ch.attr(pnkey);
        if (pnattribute == null) {
            return;
        }
        String peerName = pnattribute.get();
        if (StringUtil.isEmpty(peerName)) {
            return;
        }
        synchronized (peerName) {
            if (!channels.containsKey(peerName)) {
                return;
            }
            AttributeKey<UserPrincipal> upKey = AttributeKey.valueOf("Peer-UserPrincipal");
            UserPrincipal userPrincipal = ch.attr(upKey).get();
            if (userPrincipal != null) {
                userIndex.remove(userPrincipal.getName());
            }
            if (peerEvent != null) {
                try {
                    peerEvent.offline(peerName, userPrincipal, ch, this);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            channels.remove(peerName);
        }
    }

    @Override
    public void cast(Channel from, NetworkFrame frame) {
        switch (config.getCastmode()) {
            case "unicast":
                unicast(from, frame.hashCode(), frame);
                break;
            case "multicast":
                multicast(from, frame);
                break;
            case "feedbackcast":
                feedbackcast(from, frame);
                break;
            case "selectcast":
                selectcast(from, frame);
                break;
            default:
                break;
        }
    }


    private void feedbackcast(Channel source, NetworkFrame frame) {
        String netprotcol = getNetProtocol(source);
        Object msg = packFrame(netprotcol, frame);
        source.writeAndFlush(msg);

    }

    private ByteBuf boxFrameForTcp(NetworkFrame frame) {
        PackFrame pack = new PackFrame((byte) 1, frame);
        byte[] box = TcpFrameBox.box(pack.toBytes());
        pack.dispose();
        ByteBuf bb = Unpooled.buffer();
        bb.writeBytes(box, 0, box.length);
        return bb;
    }

    private BinaryWebSocketFrame boxFrameForWebsocket(NetworkFrame frame) {
        ByteBuf bb = Unpooled.buffer();
        byte[] b = frame.toBytes();
        frame.dispose();
        bb.writeBytes(b, 0, b.length);
        return new BinaryWebSocketFrame(bb);
    }

    private Object packFrame(String netprotcol, NetworkFrame frame) {
        Object msg = null;
        switch (netprotcol) {
            case "tcp":
                return boxFrameForTcp(frame);
            case "websocket":
                return boxFrameForWebsocket(frame);
            default:
                throw new EcmException("不支持的网络协议：" + netprotcol);
        }
    }


    private String getNetProtocol(Channel ch) {
        Attribute<String> attribute = ch.attr(AttributeKey.valueOf("Net-Protocol"));
        if (attribute == null) return "";
        return attribute.get();
    }

    @Override
    public boolean existsChannel(Channel channel) {
        return this.channels.values().contains(channel);
    }

    private void multicast(Channel from, NetworkFrame frame) {
        for (Channel ch : channels.values()) {
            if (ch == null || from.equals(ch)) {//不发自身
                continue;
            }
            if (!ch.isWritable()) {
                ch.close();
                removeChannel(ch);
            }
            String netprotcol = getNetProtocol(ch);
            Object msg = packFrame(netprotcol, frame.copy());
            ch.writeAndFlush(msg);
        }
        frame.dispose();//把源形释放，这个与单播不同，因为它有原型
    }

    //frame.head("To-Peer"),frame.head("To-User")
    private void selectcast(Channel from, NetworkFrame frame) {
        String peer = frame.head("To-Peer");
        if (!StringUtil.isEmpty(peer)) {
            castToPeer(peer, from, frame);
        }
        String user = frame.head("To-User");
        if (!StringUtil.isEmpty(user)) {
            castToUser(user, from, frame);
        }
        frame.dispose();
    }

    private void castToUser(String user, Channel from, NetworkFrame frame) {
        String peer = userIndex.get(user);
        if (StringUtil.isEmpty(peer)) {
            return;
        }
        Channel ch = channels.get(peer);
        if (ch == null) return;
        if (!ch.isWritable()) {
            ch.close();
            removeChannel(ch);
            return;
        }
        String netprotcol = getNetProtocol(ch);
        Object msg = packFrame(netprotcol, frame.copy());
        ch.writeAndFlush(msg);
    }


    private void castToPeer(String peer, Channel from, NetworkFrame frame) {
        Channel ch = channels.get(peer);
        if (ch == null) return;
        if (!ch.isWritable()) {
            ch.close();
            removeChannel(ch);
            return;
        }
        String netprotcol = getNetProtocol(ch);
        Object msg = packFrame(netprotcol, frame.copy());
        ch.writeAndFlush(msg);
    }

    private void unicast(Channel from, int hc, NetworkFrame frame) {
        if (channels.isEmpty()) {
            return;
        }
        Channel one = null;
        boolean found = false;
        for (int i = 0; i < channels.size(); i++) {
            one = channels.get((hc + i) % channels.size());
            if (one == null || from.equals(one)) {//不发自身
                continue;
            }
            if (!one.isWritable()) {
                one.close();
                removeChannel(one);
                continue;
            }
            found = true;
            break;
        }
        if (!found || one == null) {
            return;
        }
        String netprotcol = getNetProtocol(one);
        Object msg = packFrame(netprotcol, frame);
        one.writeAndFlush(msg);
    }
}
