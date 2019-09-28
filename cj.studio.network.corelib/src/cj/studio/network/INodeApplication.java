package cj.studio.network;

import cj.studio.ecm.IServiceProvider;
import cj.studio.util.reactor.IPipeline;
import cj.studio.util.reactor.IReactor;
import io.netty.channel.Channel;

/**
 * 节点应用程序<br>
 * <pre>
 *         - 认证和访问控制
 *         - 拦截请求并处理和向后传递
 *         - 对接其它节点
 *     </pre>
 */
public interface INodeApplication {
    /**
     * 根据认证模式创建认证策略，每次请求认证时触发<br>
     * 如果每次返回同一个策略实例的话则使用并发控制。<br>
     * 可以为每个网络定义专属认证策略
     *
     * @param authMode
     * @param network
     * @return
     */
    IAuthenticateStrategy createAuthenticateStrategy(String authMode, INetwork network);

    /**
     * 触发失活网络
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
     * 创建访问控制策略
     * @return
     */
    IAccessControllerStrategy createAccessControllerStrategy();

    /**
     * 节点应用启动时触发
     * @param home 应用根目录
     * @param masterNetworkName 主网络名
     * @param site 服务站点
     */
    void onstart(String home, String masterNetworkName,  IServiceProvider site);

    /**
     * 是否激活基于角色的访问控制
     * @return
     */
    boolean isEnableRBAC();

    /**
     * peer上线
     * @param peerName
     * @param userPrincipal
     * @param ch
     */
    void onlinePeer(String peerName, UserPrincipal userPrincipal, Channel ch);

    /**
     * peer下线
     * @param peerName
     * @param userPrincipal
     * @param ch
     */
    void offlinePeer(String peerName, UserPrincipal userPrincipal, Channel ch);

}
