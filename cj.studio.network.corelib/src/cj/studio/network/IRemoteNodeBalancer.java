package cj.studio.network;

/**
 * 远程节点均衡器
 */
public interface IRemoteNodeBalancer {
    /**
     * 获取远程节点的存根
     * @param key 均衡键
     * @return
     */
    INodeStub getNode(String key);
    boolean available();
}
