package cj.studio.network.node;

import cj.studio.network.INetwork;
import io.netty.channel.Channel;

public interface INetworkContainer {
    String getMasterNetworkName();
    /**
     * 是否存在网络
     * @param networkName
     * @return
     */
    boolean existsNetwork(String networkName);
    boolean isAutoCreateNetwork();

    /**
     * 在channel失活时应从各个个network中移除该channel
     * @param channel
     */
    void onChannelInactive(Channel channel);

    INetwork getNetwork(String networkName);
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


    INetwork getMasterNetwork();

}
