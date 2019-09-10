package cj.studio.network.nodeapp;

import cj.studio.util.reactor.IRemoteServiceNodeRouter;
import cj.studio.util.reactor.RemoteServiceNode;
import consistenthash.ConsistentHashRouter;

import java.rmi.Remote;

class DefaultRemoteServiceNodeRouter implements INodeRemoteServiceNodeRouter {
    ConsistentHashRouter<RemoteServiceNode> invalids;//无效节点，当被自动恢复时再次启用
    ConsistentHashRouter<RemoteServiceNode> router;
    boolean isInit;
    int vNodeCount;
    private boolean available;


    @Override
    public void init(int vNodeCount) {
        this.vNodeCount=vNodeCount;
        router = new ConsistentHashRouter<RemoteServiceNode>(null, vNodeCount);
        invalids = new ConsistentHashRouter<RemoteServiceNode>(null, vNodeCount);
        isInit=true;
    }

    @Override
    public void available(boolean b) {
        this.available=b;
    }

    @Override
    public boolean available() {
        return available;
    }

    @Override
    public int getExistingReplicas(RemoteServiceNode remoteServiceNode) {
        return router.getExistingReplicas( remoteServiceNode);
    }
    @Override
    public void removeNode(RemoteServiceNode remoteServiceNode) {
         router.removeNode( remoteServiceNode);
    }
    @Override
    public void addNode(RemoteServiceNode remoteServiceNode){
        router.addNode(remoteServiceNode,vNodeCount);
    }
    @Override
    public void invalidNode(RemoteServiceNode remoteServiceNode){
        router.removeNode(remoteServiceNode);
        invalids.addNode(remoteServiceNode,vNodeCount);
    }
    @Override
    public void validNode(RemoteServiceNode remoteServiceNode){
        invalids.removeNode(remoteServiceNode);
        router.addNode(remoteServiceNode,vNodeCount);
    }
    @Override
    public boolean isInit() {
        return isInit;
    }

    @Override
    public void dispose() {
        router=null;
        isInit=false;
    }

    @Override
    public RemoteServiceNode routeNode(String key) {
        return router.routeNode(key);
    }
}