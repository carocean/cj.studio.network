package cj.studio.network.node;

import cj.studio.ecm.*;
import cj.studio.network.*;
import cj.studio.util.reactor.IPipeline;
import cj.studio.util.reactor.IReactor;
import io.netty.channel.Channel;

import java.io.File;
import java.io.FilenameFilter;

public class NetworkNodeAppManager implements INetworkNodeAppManager {
    private final cj.studio.util.reactor.IServiceProvider parent;
    INodeApplication nodeApp;
    INetworkContainer container;
    public NetworkNodeAppManager(cj.studio.util.reactor.IServiceProvider parent) {
        this.parent = parent;
    }

    @Override
    public void load(INetworkNodeConfig config) {
        container=parent.getService("$.network.container");
        scanAssemblyAndLoad(config);
    }

    private void scanAssemblyAndLoad(INetworkNodeConfig config) {
        String home = config.home();
        String appDir = String.format("%s%sapp", home, File.separator);
        File dir = new File(appDir);
        if (!dir.exists()) {
            throw new EcmException("程序集目录不存在:" + dir);
        }
        File[] assemblies = dir.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });
        if (assemblies.length == 0) {
            throw new EcmException("缺少程序集:" + home);
        }
        if (assemblies.length > 1) {
            throw new EcmException("定义了多个程序集:" + home);
        }
        String fn = assemblies[0].getAbsolutePath();
        IAssembly app = Assembly.loadAssembly(fn);
        app.start();
        this.nodeApp = (INodeApplication) app.workbin().part("$.cj.studio.node.app");
        if (nodeApp == null) {
            throw new EcmException("程序集验证失败，原因：未发现INodeApplication 的派生实现,请检查入口服务名：$.cj.studio.node.app");
        }
        IServiceProvider site = new NodeAppServiceSite(parent);
        this.nodeApp.onstart(appDir, config.getMasterNetwork(), site);
        CJSystem.logging().info(getClass(), String.format("isEnableRBAC=%s", isEnableRBAC()));
        CJSystem.logging().info(getClass(), String.format("节点应用已启动。"));
    }

    @Override
    public void onlinePeer(String peerName, UserPrincipal userPrincipal, Channel ch) {
        nodeApp.onlinePeer(peerName,userPrincipal,ch);
    }

    @Override
    public void offlinePeer(String peerName, UserPrincipal userPrincipal, Channel ch) {
        nodeApp.offlinePeer(peerName,userPrincipal,ch);
    }

    @Override
    public void onactivedNetwork(UserPrincipal userPrincipal, INetwork network, IPipeline pipeline) {
        nodeApp.onactivedNetwork(userPrincipal, network, pipeline);
    }

    @Override
    public void oninactiveNetwork(INetwork network, IPipeline pipeline) {
        nodeApp.oninactiveNetwork(network, pipeline);
    }

    @Override
    public IAuthenticateStrategy createAuthenticateStrategy(String authMode, INetwork network) {
        return nodeApp.createAuthenticateStrategy(authMode, network);
    }

    @Override
    public IAccessControllerStrategy createAccessControllerStrategy() {
        return nodeApp.createAccessControllerStrategy();
    }

    @Override
    public boolean isEnableRBAC() {
        return nodeApp.isEnableRBAC();
    }

    private class NodeAppServiceSite implements IServiceProvider {
        private  IReactor reactor;
        //用parent向app开放有限系统服务
        cj.studio.util.reactor.IServiceProvider parent;
        public NodeAppServiceSite(cj.studio.util.reactor.IServiceProvider parent) {
            this.parent=parent;
        }

        @Override
        public <T> ServiceCollection<T> getServices(Class<T> serviceClazz) {

            return null;
        }

        @Override
        public Object getService(String serviceId) {
            int pos=serviceId.indexOf("$.network.name.");
            if(pos==0){
                String networkName=serviceId.substring("$.network.name.".length(),serviceId.length());
                return container.getNetwork(networkName);
            }
            return null;
        }
    }
}
