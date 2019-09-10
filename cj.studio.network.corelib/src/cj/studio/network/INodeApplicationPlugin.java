package cj.studio.network;

import cj.studio.ecm.IServiceProvider;
import cj.studio.util.reactor.IPipeline;

/**
 * 开放插件供第三方开发者使用,该插件仅供拦截请求侦并处理转换侦<br>
 *     一个节点下可支持多个应用插件
 */
public interface INodeApplicationPlugin {

    /**
     * 触发失活网络,
     * @param network
     * @param pipeline
     */
    void oninactiveNetwork(INetwork network, IPipeline pipeline);

    /**
     * 触发激动网络，即第一次有请求使用网络时触发
     * @param userPrincipal
     * @param network
     * @param pipeline
     */
    void onactivedNetwork(UserPrincipal userPrincipal, INetwork network, IPipeline pipeline);


    /**
     * 节点应用启动时触发
     * @param masterNetworkName 主网络名
     * @param site 服务站点
     */
    void onstart(String masterNetworkName,  IServiceProvider site);

}
