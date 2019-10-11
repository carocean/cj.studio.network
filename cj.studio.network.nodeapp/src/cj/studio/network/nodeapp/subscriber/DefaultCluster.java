package cj.studio.network.nodeapp.subscriber;

import cj.studio.ecm.CJSystem;
import cj.studio.network.nodeapp.DefaultRemoteServiceNodeRouter;
import cj.studio.network.nodeapp.INodeRemoteServiceNodeRouter;
import cj.studio.network.peer.IPeer;
import cj.studio.util.reactor.RemoteServiceNode;
import cj.ultimate.util.StringUtil;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class DefaultCluster implements ICluster {

    INodeOrientor orientor;
    String balanceMode;
    Map<String, Object[]> connectedNodes;//key是peer名因为一个peer代表一个远端的node因此也可当作远程节点名，在subscribers.ymal中的nodes配置peer名;value是peer,info数组
    INodeRemoteServiceNodeRouter remoteServiceNodeRouter;
    Map<String, Object[]> invalidConnectedNodes;

    @Override
    public void init(ISubscriberConfig subscriberConfig) {
        this.connectedNodes = new HashMap<>();
        this.invalidConnectedNodes = new HashMap<>();
        int vNodeCount = subscriberConfig.getVNodeCount();
        String home = subscriberConfig.home();
        INodeRemoteServiceNodeRouter remoteServiceNodeRouter = new DefaultRemoteServiceNodeRouter();
        remoteServiceNodeRouter.init(vNodeCount);
        this.remoteServiceNodeRouter = remoteServiceNodeRouter;
        this.balanceMode = subscriberConfig.getBalance();
        remoteServiceNodeRouter.available(!"none".equals(subscriberConfig.getBalance()) && !subscriberConfig.getSubscribers().isEmpty());
        CJSystem.logging().info(getClass(), String.format("Cluster均衡策略为：%s", balanceMode));
        if ("orientor".equals(balanceMode)) {
            if (!home.endsWith(File.separator)) {
                home = home + File.separator;
            }
            String dbfile = String.format("%sorientor%sorient", home, File.separator);
            File file = new File(dbfile);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            this.orientor = new DefaultNodeOrientor(dbfile);
            CJSystem.logging().info(getClass(), String.format("定向器的数据目录：%s", dbfile));
        }
    }


    @Override
    public synchronized void validNode(String nodeName) {
        //如果没有调用过使无效的方法可能在有效列表中存在
        if (this.connectedNodes.containsKey(nodeName)) return;

        Object[] objs = this.invalidConnectedNodes.get(nodeName);
        if (objs == null) return;
        invalidConnectedNodes.remove(nodeName);
        connectedNodes.put(nodeName, objs);

        IPeer peer = (IPeer) objs[0];
        SubscriberInfo info = (SubscriberInfo) objs[1];
        RemoteServiceNode remoteServiceNode = new RemoteServiceNode(peer.peerName(), info.getNodeAddress());
        if (remoteServiceNodeRouter.getExistingReplicas(remoteServiceNode) < 1) {//如果存在至少有1个虚节点
            remoteServiceNodeRouter.addNode(remoteServiceNode);
        }
    }

    @Override
    public synchronized void invalidNode(String nodeName) {
        Object[] objs = connectedNodes.get(nodeName);
        if (objs == null) return;
        invalidConnectedNodes.put(nodeName, objs);
        connectedNodes.remove(nodeName);

        IPeer peer = (IPeer) objs[0];
        SubscriberInfo info = (SubscriberInfo) objs[1];
        RemoteServiceNode remoteServiceNode = new RemoteServiceNode(peer.peerName(), info.getNodeAddress());
        if (remoteServiceNodeRouter.getExistingReplicas(remoteServiceNode) > 0) {
            remoteServiceNodeRouter.removeNode(remoteServiceNode);
        }
    }

    @Override
    public boolean available() {
        return remoteServiceNodeRouter.available();
    }

    @Override
    public synchronized Object[] getNode(String objkey) {
        Object[] ret = null;
        switch (this.balanceMode) {
            case "orientor":
                String nodeName = orientor.get(objkey);
                if (StringUtil.isEmpty(nodeName)) {
                    RemoteServiceNode node = remoteServiceNodeRouter.routeNode(objkey);
                    if(node==null)return null;//当节点都无效时可能路由器会返回空
                    nodeName = node.getKey();
                    orientor.set(objkey, nodeName);
                }
                ret = connectedNodes.get(nodeName);
                break;
            case "unorientor":
                RemoteServiceNode node = remoteServiceNodeRouter.routeNode(objkey);
                if(node==null)return null;//当节点都无效时可能路由器会返回空
                ret = connectedNodes.get(node.getKey());
                break;
        }
        return ret;
    }

    @Override
    public synchronized void addNode(IPeer peer, SubscriberInfo info) {
        RemoteServiceNode remoteServiceNode = new RemoteServiceNode(peer.peerName(), info.getNodeAddress());
        remoteServiceNodeRouter.addNode(remoteServiceNode);//添加负载节点
        connectedNodes.put(peer.peerName(), new Object[]{peer, info});
    }
}
