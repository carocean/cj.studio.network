package cj.studio.network;

import cj.studio.ecm.IServiceProvider;
import cj.studio.util.reactor.IPipeline;
import io.netty.channel.Channel;

/**
 * 开放插件供第三方开发者使用,该插件仅供拦截请求侦并处理转换侦<br>
 *     一个节点下可支持多个应用插件
 */
public interface INodeApplicationPlugin {

    /**
     * 触发失活网络,
     * @param network
     * @param pipeline
     * @param remoteNodeBalancer
     */
    void oninactiveNetwork(INetwork network, IPipeline pipeline, IRemoteNodeBalancer remoteNodeBalancer);

    /**
     * 触发激动网络，即第一次有请求使用网络时触发
     * @param userPrincipal
     * @param network
     * @param pipeline
     * @param remoteNodeBalancer
     */
    void onactivedNetwork(UserPrincipal userPrincipal, INetwork network, IPipeline pipeline, IRemoteNodeBalancer remoteNodeBalancer);


    /**
     * 节点应用启动时触发
     * @param masterNetworkName 主网络名
     * @param site 服务站点
     */
    void onstart(String masterNetworkName,  IServiceProvider site);

    /**
     * peer上线
     * @param peerName 客户端端名
     * @param userPrincipal 用户信息
     * @param source 事件源，即在哪个管道上发生的事件
     * @param network 事件发生的网络
     * @param remoteNodeBalancer 远程节点均衡器
     */
    void onlinePeer(String peerName, UserPrincipal userPrincipal, Channel source, INetwork network, IRemoteNodeBalancer remoteNodeBalancer);

    /**
     * peer下线
     * @param peerName 客户端端名
     * @param userPrincipal 用户信息
     * @param source 事件源，即在哪个管道上发生的事件
     * @param network 事件发生的网络
     * @param remoteNodeBalancer 远程节点均衡器
     */
    void offlinePeer(String peerName, UserPrincipal userPrincipal, Channel source, INetwork network, IRemoteNodeBalancer remoteNodeBalancer);

}
