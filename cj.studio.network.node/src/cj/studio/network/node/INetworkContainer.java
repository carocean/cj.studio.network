package cj.studio.network.node;

import cj.studio.network.INetwork;
import cj.studio.network.NetworkInfo;
import io.netty.channel.Channel;

public interface INetworkContainer {

    /**
     * 是否存在网络
     * @param networkName
     * @return
     */
    boolean existsNetwork(String networkName);

    /**
     * 获取用于管理方网络信息
     * @return
     */
    NetworkInfo getManagerNetworkInfo();

    /**
     * 在channel失活时应从各个个network中移除该channel
     * @param channel
     */
    void onChannelInactive(Channel channel);

    INetwork getNetwork(String networkName);
    INetwork getManagerNetwork();
    /**
     * 移除网络
     * @param networkName
     */
    void removeNetwork(String networkName);

    /**
     * 创建网络
     * @param name
     * @param castmode
     * @return
     */
    INetwork createNetwork(String name, String castmode);

    String[] enumNetworkName(boolean isSorted);

    void renameNetwork(String networkName, String newNetworkName);

    void changeNetworkCastmode(String networkName, String castmode);

}
