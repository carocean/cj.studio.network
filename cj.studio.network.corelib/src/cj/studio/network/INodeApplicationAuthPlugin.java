package cj.studio.network;

import cj.studio.ecm.IServiceProvider;

/**
 * 该插件专门用于替代系统app的认证能力，实现第三方开发认证。一个节点下仅支持一个认证插件
 */
public interface INodeApplicationAuthPlugin {


    /**
     * 节点应用启动时触发
     *
     * @param authHomeDir
     * @param masterNetworkName 主网络名
     * @param site              服务站点
     */
    void onstart(String authHomeDir, String masterNetworkName, IServiceProvider site);

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
     * 创建访问控制策略
     *
     * @return
     */
    IAccessControllerStrategy createAccessControllerStrategy();
}
