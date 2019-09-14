package cj.studio.network.nodeapp.subscriber;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.net.CircuitException;
import cj.studio.network.NetworkFrame;
import cj.studio.network.nodeapp.INodeRemoteServiceNodeRouter;
import cj.studio.network.peer.INetworkPeer;
import cj.studio.network.peer.INetworkPeerContainer;
import cj.studio.network.peer.IPeer;
import cj.studio.util.reactor.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 向后端负载，仅且支持单播，因多多播可以通过订阅机制实现
 */
public class ClusterValve implements IValve {
    INodeRemoteServiceNodeRouter remoteServiceNodeRouter;
    IOrientor orientor;
    String clusterBalanceMode;
    Map<String, Object> extraMap;

    public ClusterValve(INodeRemoteServiceNodeRouter remoteServiceNodeRouter, String clusterBalanceMode, String home) {
        this.clusterBalanceMode = clusterBalanceMode;
        this.remoteServiceNodeRouter = remoteServiceNodeRouter;
        extraMap = new HashMap<>();
        CJSystem.logging().info(getClass(), String.format("Cluster均衡策略为：%s", clusterBalanceMode));
        if ("orientor".equals(clusterBalanceMode)) {
            if (!home.endsWith(File.separator)) {
                home = home + File.separator;
            }
            String dbfile = String.format("%sorientor%sorient", home,File.separator);
            File file=new File(dbfile);
            if(!file.getParentFile().exists()){
                file.getParentFile().mkdirs();
            }
            this.orientor = new DefaultOrientor(dbfile);
            CJSystem.logging().info(getClass(), String.format("定向器的数据目录：%s", dbfile));
        }

    }

    @Override
    public void flow(Event e, IPipeline pipeline) throws CircuitException {
        if (remoteServiceNodeRouter.available()) {
            castRemoteNode(e, pipeline);
        }
        pipeline.nextFlow(e, this);
    }


    private void castRemoteNode(Event e, IPipeline pipeline) throws CircuitException {
        RemoteServiceNode node = selectRoutting(e, pipeline);
        if (node == null) {
            return;
        }
        Object[] arr = (Object[]) node.getExtra();
        if (arr == null) return;
        IPeer peer = (IPeer) arr[0];
        SubscriberInfo info = (SubscriberInfo) arr[1];
        INetworkPeerContainer container = (INetworkPeerContainer) peer.site().getService("$.peer.container");
        NetworkFrame frame = (NetworkFrame) e.getParameters().get("frame");
        boolean hasInvalidNode = false;
        for (SubscribeNetwork sn : info.getSubscribeNetworks()) {
            INetworkPeer networkPeer = container.get(sn.network);
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
            remoteServiceNodeRouter.invalidNode(node);//使该节点无效,一次掉包或出错都不可以,当peer重连成功时会节点恢复
            castRemoteNode(e, pipeline);
        }
    }

    private RemoteServiceNode selectRoutting(Event e, IPipeline pipeline) {
        RemoteServiceNode node = null;
        switch (clusterBalanceMode) {
            case "orientor":
                node = orientor.get(e.getKey());
                Object extra = extraMap.get(e.getKey());
                if (extra != null) {
                    node.setExtra(extra);
                }
                if (node == null) {
                    node = remoteServiceNodeRouter.routeNode(e.getKey());
                    extraMap.put(e.getKey(), node.getExtra());
                    node.setExtra(null);
                    orientor.set(e.getKey(), node);
                }
                break;
            case "unorientor":
                node = remoteServiceNodeRouter.routeNode(e.getKey());
                break;
        }
        return node;
    }

    @Override
    public void nextError(Event e, Throwable error, IPipeline pipeline) throws CircuitException {
        pipeline.nextError(e, error, this);
    }
}
