package cj.studio.network.nodeapp.subscriber;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.net.CircuitException;
import cj.studio.network.INetwork;
import cj.studio.network.NetworkFrame;
import cj.studio.network.peer.INetworkPeer;
import cj.studio.network.peer.INetworkPeerContainer;
import cj.studio.network.peer.IPeer;
import cj.studio.util.reactor.*;

/**
 * 向后端负载，仅且支持单播，因多多播可以通过订阅机制实现
 */
public class ClusterValve implements IValve {
    IRemoteServiceNodeRouter remoteServiceNodeRouter;

    public ClusterValve(IRemoteServiceNodeRouter remoteServiceNodeRouter) {
        this.remoteServiceNodeRouter = remoteServiceNodeRouter;
    }

    @Override
    public void flow(Event e, IPipeline pipeline) throws CircuitException {
        try {
            castRemoteNode(e);
        }catch (Throwable throwable){
            nextError(e,new CircuitException("500","请求侦在向其它远程节点分发时出错:"+throwable),pipeline);
        }
        pipeline.nextFlow(e, this);
    }


    private void castRemoteNode(Event e) throws CircuitException {
        RemoteServiceNode node = remoteServiceNodeRouter.routeNode(e.getKey());
        if (node == null) {
            return;
        }
        String peername = node.getKey();
        //SubscriberInfo
        Object[] arr=(Object[]) node.getExtra();
        if (arr == null) return;
        IPeer peer = (IPeer)arr[0];
        SubscriberInfo info=(SubscriberInfo)arr[1];
        INetworkPeerContainer container=(INetworkPeerContainer) peer.site().getService("$.peer.container");
        NetworkFrame frame=(NetworkFrame)e.getParameters().get("frame");
        for(SubscribeNetwork sn:info.getSubscribeNetworks()){
           INetworkPeer networkPeer=container.get(sn.network);
           if(networkPeer==null)continue;
           networkPeer.send(frame.copy());
        }
//        frame.dispose();
    }

    @Override
    public void nextError(Event e, Throwable error, IPipeline pipeline) throws CircuitException {
        pipeline.nextError(e, error, this);
    }
}
