package cj.studio.network.nodeapp;

import cj.studio.ecm.CJSystem;
import cj.studio.network.INodeStub;
import cj.studio.network.IRemoteNodeBalancer;
import cj.studio.network.NetworkCircuit;
import cj.studio.network.NetworkFrame;
import cj.studio.network.nodeapp.subscriber.ICluster;
import cj.studio.network.peer.*;

public class DefaultRemoteNodeBalancer implements IRemoteNodeBalancer {
    private final ICluster cluster;

    public DefaultRemoteNodeBalancer(ICluster cluster) {
        this.cluster = cluster;
    }

    @Override
    public INodeStub getNode(String key) {
        Object[] node = cluster.getNode(key);
        if (node == null) {
            return null;
        }
        IPeer peer = (IPeer) node[0];
        return new DefaultNodeStub(peer);
    }

    @Override
    public boolean available() {
        return cluster.available();
    }

    private class DefaultNodeStub implements INodeStub, IOnopen, IOnerror,IOnmessage,IOnclose {
        IPeer peer;


        public DefaultNodeStub(IPeer peer) {
            this.peer = peer;
        }

        @Override
        public String peerName() {
            return peer.peerName();
        }

        @Override
        public String getNodeHost() {
            return peer.getNodeHost();
        }

        @Override
        public String getNodeProtocol() {
            return peer.getNodeProtocol();
        }

        @Override
        public int getNodePort() {
            return peer.getNodePort();
        }

        @Override
        public void send(String networkName,NetworkFrame frame) {
            INetworkPeerContainer container = (INetworkPeerContainer) peer.site().getService("$.peer.container");
            INetworkPeer networkPeer = container.get(networkName);
            if(networkPeer==null){//如果现有容器中没有订阅该网络，则侦听它
                networkPeer=peer.listen(networkName,this,this,this,this);
            }
            networkPeer.send(frame);
        }

        @Override
        public void onclose(INetworkPeer networkPeer) {
            CJSystem.logging().info(getClass(),String.format("关闭远程网络:%s",networkPeer.getNetworkName()));
        }

        @Override
        public void onerror(NetworkFrame frame, NetworkCircuit circuit, INetworkPeer networkPeer) {
            CJSystem.logging().info(getClass(),String.format("远程网络错误:%s",networkPeer.getNetworkName()));
        }

        @Override
        public void onmessage(NetworkFrame frame, INetworkPeer networkPeer) {
            CJSystem.logging().info(getClass(),String.format("收到远程网络消息:%s",networkPeer.getNetworkName()));
        }

        @Override
        public void onopen(NetworkFrame frame, INetworkPeer networkPeer) {
            CJSystem.logging().info(getClass(),String.format("打开远程网络:%s",networkPeer.getNetworkName()));
        }
    }
}
