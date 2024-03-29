package cj.studio.network.nodeapp.subscriber;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;
import cj.studio.network.INetwork;
import cj.studio.network.NetworkCircuit;
import cj.studio.network.NetworkFrame;
import cj.studio.network.peer.*;
import cj.studio.network.peer.connection.IOnReconnectEvent;
import cj.ultimate.gson2.com.google.gson.Gson;
import io.netty.channel.Channel;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

public class SubscriberContainer implements ISubscriberContainer {
    ISubscriberConfig subscriberConfig;
    IServiceProvider site;
    ICluster cluster;

    public SubscriberContainer(ICluster cluster) {
        this.cluster = cluster;
    }

    @Override
    public ISubscriberConfig getSubscriberConfig() {
        return subscriberConfig;
    }

    @Override
    public void start(String home, IServiceProvider site) {
        this.site = site;
        subscriberConfig = new SubscriberConfig();
        try {
            subscriberConfig.load(home);
        } catch (FileNotFoundException e) {
            throw new EcmException(e);
        }
        cluster.init(subscriberConfig);
        subscribe();
    }

    protected void subscribe() {
        for (SubscriberInfo info : subscriberConfig.getSubscribers()) {
            try {
                IPeer peer = Peer.create(info.getPeerName(), site);
                peer.connect(info.getNodeAddress(), info.getMasterNetworkName(), new MonitorOnReconnectEvent(cluster, info.getPeerName()));
                ISubscriberEvent masterEvent = new MasterEvent(cluster, peer, info);
                peer.auth(info.getAuthMode(), info.getUser(), info.getToken(), masterEvent, masterEvent, masterEvent, masterEvent);
                CJSystem.logging().info(getClass(), String.format("以Peer:%s 已连接到节点：%s, 主网是：%s", info.getPeerName(), info.getNodeAddress(), info.getMasterNetworkName()));
            } catch (Throwable throwable) {
                CJSystem.logging().error(getClass(), throwable.getMessage());
                continue;
            }
        }
    }

    private class MasterEvent implements ISubscriberEvent {
        IPeer peer;
        SubscriberInfo info;
        ICluster cluster;

        public MasterEvent(ICluster cluster, IPeer peer, SubscriberInfo info) {
            this.peer = peer;
            this.info = info;
            this.cluster=cluster;
        }

        @Override
        public void onopen(NetworkFrame frame, INetworkPeer networkPeer) {
            for (SubscribeNetwork subscribeNetwork : info.getSubscribeNetworks()) {
                ISubscriberEvent workEvent = new WorkEvent(info);
                peer.listen(subscribeNetwork.network, workEvent, workEvent, workEvent, workEvent);
                CJSystem.logging().info(getClass(), String.format("----已订阅网络：%s, 分发给本地网络：%s", subscribeNetwork.network, new Gson().toJson(subscribeNetwork.castToLocals)));
            }
            cluster.addNode(peer, info);

            StringBuffer sb = new StringBuffer();
            sb.append(String.format("网络：%s 已打开", networkPeer.getNetworkName()));
            CJSystem.logging().info(getClass(), sb);
        }

        @Override
        public void onclose(INetworkPeer networkPeer) {
//            IPeer peer = (IPeer) networkPeer.site().getService("$.peer");
//            if (peer != null) {
//                remoteServiceNodeRouter.removeNode(new RemoteServiceNode(peer.peerName(), ""));
//            }
            StringBuffer sb = new StringBuffer();
            sb.append(String.format("网络：%s 已关闭", networkPeer.getNetworkName()));
            CJSystem.logging().info(getClass(), sb);
        }

        @Override
        public void onerror(NetworkFrame frame, NetworkCircuit circuit, INetworkPeer networkPeer) {
            StringBuffer sb = new StringBuffer();
            circuit.print(sb);
            CJSystem.logging().info(getClass(), sb);
        }

        @Override
        public void onmessage(NetworkFrame frame, INetworkPeer networkPeer) {
            byte[] b = frame.content().readFully();
            NetworkCircuit circuit = new NetworkCircuit(b);
            if ("auth".equals(frame.command()) && circuit.status().equals("200")) {
                CJSystem.logging().info(getClass(), String.format("Peer:%s 成功认证", networkPeer.getNetworkName()));
                return;
            }
            StringBuffer sb = new StringBuffer();
            circuit.print(sb);
            CJSystem.logging().info(getClass(), sb);
        }

    }

    private class WorkEvent implements ISubscriberEvent {
        SubscriberInfo info;
        Map<String, INetwork> refNetworks;//缓存对network的引用，由于每个network复用线程而避免采用并发控制

        public WorkEvent(SubscriberInfo info) {
            this.info = info;
            refNetworks = new HashMap<>();
        }

        @Override
        public void onclose(INetworkPeer networkPeer) {
            StringBuffer sb = new StringBuffer();
            sb.append(String.format("网络：%s 已关闭", networkPeer.getNetworkName()));
            CJSystem.logging().info(getClass(), sb);
        }

        @Override
        public void onerror(NetworkFrame frame, NetworkCircuit circuit, INetworkPeer networkPeer) {
            StringBuffer sb = new StringBuffer();
            circuit.print(sb);
            CJSystem.logging().info(getClass(), sb);
        }

        @Override
        public void onmessage(NetworkFrame frame, INetworkPeer networkPeer) {
            if (!info.containsSubscriberNetwork(networkPeer.getNetworkName())) {
                return;
            }

            SubscribeNetwork subscribeNetwork = info.getSubscriberNetwork(networkPeer.getNetworkName());
            Channel channel = (Channel) networkPeer.site().getService("$.peer.channel");
            String url = frame.url();
            for (String sn : subscribeNetwork.getCastToLocals()) {
                INetwork network = refNetworks.get(sn);
                if (network == null) {
                    network = (INetwork) site.getService(String.format("$.network.name.%s", sn));
                    if (network == null) {
                        continue;
                    }
                    network = network.createReference();
                    refNetworks.put(sn, network);
                }
                NetworkFrame copy = frame.copy();
                copy.url(String.format("/%s%s", network.getInfo().getName(), url));
                network.cast(channel, copy);
            }
//            frame.dispose();
        }

        @Override
        public void onopen(NetworkFrame frame, INetworkPeer networkPeer) {
            StringBuffer sb = new StringBuffer();
            sb.append(String.format("网络：%s 已打开", networkPeer.getNetworkName()));
            CJSystem.logging().info(getClass(), sb);
        }
    }

    private class MonitorOnReconnectEvent implements IOnReconnectEvent {
        String nodeName;
        ICluster cluster;

        public MonitorOnReconnectEvent(ICluster cluster, String nodeName) {
            this.nodeName = nodeName;
            this.cluster = cluster;
        }

        @Override
        public void onreconnect() {
            cluster.validNode(nodeName);
        }

        @Override
        public void init(String authmode, String user, String token, IOnerror onerror, IOnopen onopen, IOnmessage onmessage, IOnclose onclose) {

        }
    }
}
