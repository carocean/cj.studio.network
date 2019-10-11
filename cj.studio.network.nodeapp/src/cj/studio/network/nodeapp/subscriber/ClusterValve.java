package cj.studio.network.nodeapp.subscriber;

import cj.studio.ecm.net.CircuitException;
import cj.studio.network.NetworkFrame;
import cj.studio.network.peer.INetworkPeer;
import cj.studio.network.peer.INetworkPeerContainer;
import cj.studio.network.peer.IPeer;
import cj.studio.util.reactor.*;

/**
 * 向后端负载，仅且支持单播，因多多播可以通过订阅机制实现
 */
public class ClusterValve implements IValve {

    ICluster cluster;

    public ClusterValve(ICluster cluster) {
        this.cluster=cluster;
    }

    @Override
    public void flow(Event e, IPipeline pipeline) throws CircuitException {
        if (cluster.available()) {
            castRemoteNode(e, pipeline);
        }
        pipeline.nextFlow(e, this);
    }


    private void castRemoteNode(Event e, IPipeline pipeline) throws CircuitException {
        Object[] node = cluster.getNode(e.getKey());
        if (node == null) {
            return;
        }
        IPeer peer = (IPeer) node[0];
        SubscriberInfo info = (SubscriberInfo) node[1];
        INetworkPeerContainer container = (INetworkPeerContainer) peer.site().getService("$.peer.container");
        NetworkFrame frame = (NetworkFrame) e.getParameters().get("frame");
        boolean hasInvalidNode = false;
        for (SubscribeNetwork sn : info.getSubscribeNetworks()) {
            INetworkPeer networkPeer = container.get(sn.getNetwork());
            if (networkPeer == null) continue;
            NetworkFrame copy = frame.copy();
            //看到这了吧，看到这就对了。网络有个特点：除主网络外，发送给工作网络的侦默认不会被返回自身，除非指定网络类型为feedbackcast类型，因此，为什么发送给上游的消息不见回馈到本节点的网络？就是这个原因。
            String relativeUrl = copy.relativeUrl();
            copy.url(relativeUrl);
            try {
                networkPeer.send(copy);
            } catch (Throwable throwable) {
                hasInvalidNode = true;
                CircuitException ce = CircuitException.search(throwable);
                if (ce != null) {
                    nextError(e, ce, pipeline);
                } else {
                    nextError(e, new CircuitException("500", throwable), pipeline);
                }
                continue;
            }
        }
        if (hasInvalidNode) {
            cluster.invalidNode(peer.peerName());//使该节点无效,一次掉包或出错都不可以,当peer重连成功时会节点恢复
            castRemoteNode(e, pipeline);
        }
    }


    @Override
    public void nextError(Event e, Throwable error, IPipeline pipeline) throws CircuitException {
        pipeline.nextError(e, error, this);
    }
}
