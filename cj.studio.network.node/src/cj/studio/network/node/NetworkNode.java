package cj.studio.network.node;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.annotation.CjService;
import cj.studio.network.node.server.TcpNetworkNodeServer;
import cj.studio.network.node.server.WebsocketNetworkNodeServer;
import cj.studio.util.reactor.IServiceProvider;

import java.io.FileNotFoundException;

@CjService(name = "networkNode", isExoteric = true)
public class NetworkNode implements INetworkNode {
    //默认一个network作为node管理和控制
    INetworkNodeServer nodeServer;//一个节点有且仅有一个服务器
    INetworkContainer networkContainer;//网络容器
    INetworkNodeConfig networkNodeConfig;//节点配置
    INetworkNodeAppManager app;//节点应用管理器
    IServiceProvider site;

    @Override
    public void entrypoint(String home) throws FileNotFoundException {
        site = new NodeServiceProvider();
        networkNodeConfig = new NetworkNodeConfig();
        networkNodeConfig.load(home);

        app = new NetworkNodeAppManager(site);

        nodeServer = createNetworkNodeServer(networkNodeConfig.getServerInfo());
        networkContainer = new NetworkContainer(site);
        nodeServer.start();

        app.load(networkNodeConfig);

        CJSystem.logging().info(getClass(), String.format("服务地址:%s", networkNodeConfig.getServerInfo()));
    }

    protected INetworkNodeServer createNetworkNodeServer(ServerInfo serverInfo) {
        switch (serverInfo.getProtocol()) {
            case "tcp":
                return new TcpNetworkNodeServer(site);
            case "ws":
            case "wss":
                return new WebsocketNetworkNodeServer(site);
            default:
                throw new EcmException(String.format("不支持的协议：%s", serverInfo.getProtocol()));
        }
    }

    class NodeServiceProvider implements IServiceProvider {
        @Override
        public <T> ServiceCollection<T> getServices(Class<T> clazz) {
            return null;
        }

        @Override
        public Object getService(String serviceId) {
            if ("$.network.container".equals(serviceId)) {
                return networkContainer;
            }
            if ("$.network.server".equals(serviceId)) {
                return nodeServer;
            }
            if ("$.network.config".equals(serviceId)) {
                return networkNodeConfig;
            }
            if ("$.network.app.manager".equals(serviceId)) {
                return app;
            }
            if ("$.reactor".equals(serviceId)) {
                return ((IServiceProvider) nodeServer).getService("$.reactor");
            }
            return null;
        }
    }
}
