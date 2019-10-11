package cj.studio.network.nodeapp.plugin.example;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.IChip;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceSite;
import cj.studio.network.*;
import cj.studio.network.peer.INetworkPeer;
import cj.studio.network.peer.INetworkPeerContainer;
import cj.studio.network.peer.IPeer;
import cj.studio.util.reactor.IPipeline;
import io.netty.channel.Channel;

@CjService(name = "$.cj.studio.node.app.plugin", isExoteric = true)
public class NodeAppPlugin implements INodeApplicationPlugin {
    @CjServiceSite
    IServiceProvider site;
    String networkGberaEvent;

    @Override
    public synchronized void onstart(String masterNetworkName, IServiceProvider site) {
        IChip chip = (IChip) this.site.getService(IChip.class.getName());
        networkGberaEvent = chip.site().getProperty("gbera.event.network");
        CJSystem.logging().info(getClass(), String.format("应用插件示例程序:%s 已启动", chip.info().getName()));
    }

    @Override
    public void onactivedNetwork(UserPrincipal userPrincipal, INetwork network, IPipeline pipeline, IRemoteNodeBalancer remoteNodeBalancer) {
        IChip chip = (IChip) this.site.getService(IChip.class.getName());
        String key = pipeline.key();
        if (!remoteNodeBalancer.available()) return;
        INodeStub node = remoteNodeBalancer.getNode(key);//根据客户端负载出远程节点node
        if (node == null) {
            return;
        }
        NetworkFrame frame = new NetworkFrame("onactivedNetwork /event gbera/1.0");
        frame.head("Principal", userPrincipal.getName());
        frame.head("Network", network.getInfo().getName());
        frame.head("Roles", userPrincipal.toRoles());
        node.send(networkGberaEvent, frame);

//        CJSystem.logging().info(getClass(), String.format("%s----onactivedNetwork", chip.info().getName()));
    }

    @Override
    public void oninactiveNetwork(INetwork network, IPipeline pipeline, IRemoteNodeBalancer remoteNodeBalancer) {
        IChip chip = (IChip) this.site.getService(IChip.class.getName());
        String key = pipeline.key();
        if (!remoteNodeBalancer.available()) return;
        INodeStub node = remoteNodeBalancer.getNode(key);//根据客户端负载出远程节点node
        if (node == null) {
            return;
        }

        NetworkFrame frame = new NetworkFrame("oninactiveNetwork /event gbera/1.0");
        frame.head("Network", network.getInfo().getName());
        node.send(networkGberaEvent, frame);
//        CJSystem.logging().info(getClass(), String.format("%s----oninactiveNetwork", chip.info().getName()));
    }

    @Override
    public void onlinePeer(String peerName, UserPrincipal userPrincipal, Channel source, INetwork network, IRemoteNodeBalancer remoteNodeBalancer) {
        if (!remoteNodeBalancer.available()) return;
        INodeStub node = remoteNodeBalancer.getNode(peerName);//根据客户端负载出远程节点node
        if (node == null) {
            return;
        }
        NetworkFrame frame = new NetworkFrame("onlinePeer /event gbera/1.0");
        frame.head("Peer", peerName);
        frame.head("Principal", userPrincipal.getName());
        frame.head("Network", network.getInfo().getName());
        frame.head("Roles", userPrincipal.toRoles());
        node.send(networkGberaEvent, frame);

//        CJSystem.logging().info(getClass(), String.format("应用插件示例程序，侦听到peer上线事件：peer:%s, user:%s", peerName, userPrincipal == null ? "" : userPrincipal.getName()));
    }

    @Override
    public void offlinePeer(String peerName, UserPrincipal userPrincipal, Channel source, INetwork network, IRemoteNodeBalancer remoteNodeBalancer) {
        if (!remoteNodeBalancer.available()) return;
        INodeStub node = remoteNodeBalancer.getNode(peerName);//根据客户端负载出远程节点node
        if (node == null) {
            return;
        }
        NetworkFrame frame = new NetworkFrame("offlinePeer /event gbera/1.0");
        frame.head("Peer", peerName);
        frame.head("Principal", userPrincipal.getName());
        frame.head("Network", network.getInfo().getName());
        frame.head("Roles", userPrincipal.toRoles());
        node.send(networkGberaEvent, frame);
//        CJSystem.logging().info(getClass(), String.format("应用插件示例程序，侦听到peer下线事件：peer:%s, user:%s", peerName, userPrincipal == null ? "" : userPrincipal.getName()));
    }

}
