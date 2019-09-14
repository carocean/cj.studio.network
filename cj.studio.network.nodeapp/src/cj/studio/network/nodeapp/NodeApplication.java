package cj.studio.network.nodeapp;

import cj.studio.ecm.*;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.annotation.CjService;
import cj.studio.network.*;
import cj.studio.network.nodeapp.subscriber.ClusterValve;
import cj.studio.network.nodeapp.strategy.PasswordAuthenticateStrategy;
import cj.studio.network.nodeapp.strategy.SystemAccessControllerStrategy;
import cj.studio.network.nodeapp.subscriber.ISubscriberContainer;
import cj.studio.network.nodeapp.subscriber.SubscriberContainer;
import cj.studio.util.reactor.*;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

@CjService(name = "$.cj.studio.node.app", isExoteric = true)
public class NodeApplication implements INodeApplication {
    IRBACConfig rbacconfig;
    String masterNetworkName;
    List<IValve> valves;
    ISubscriberContainer subscriberContainer;
    INodeRemoteServiceNodeRouter remoteServiceNodeRouter;
    INodeApplicationAuthPlugin authPlugin;
    List<INodeApplicationPlugin> plugins;
    IServiceProvider pluginSite;

    @Override
    public void onstart(String home, String masterNetworkName, IServiceProvider site) {
        pluginSite = new PluginSite(site);
        this.masterNetworkName = masterNetworkName;
        this.valves = new ArrayList<>();
        rbacconfig = new RBACConfig(masterNetworkName);
        try {
            rbacconfig.load(home);
        } catch (FileNotFoundException e) {
            throw new EcmException(e);
        }

        this.remoteServiceNodeRouter = new DefaultRemoteServiceNodeRouter();
        remoteServiceNodeRouter.init(10);
        subscriberContainer = new SubscriberContainer(remoteServiceNodeRouter);
        subscriberContainer.start(home, site);

        pluginSite = new PluginSite(site);
        scanPluginsAndLoad(home);
    }

    private void scanPluginsAndLoad(String home) {
        //先加载认证插件
        String authDir = String.format("%s%splugins%sauth", home, File.separator, File.separator);
        scanAuthPluginAndLoad(authDir);

        //再加载其它插件
        plugins = new ArrayList<>();
        String othersDir = String.format("%s%splugins%sothers", home, File.separator, File.separator);
        scanOtherPluginsAndLoad(othersDir);

    }

    private void scanOtherPluginsAndLoad(String othersDir) {
        File dir = new File(othersDir);
        if (!dir.exists()) {
            throw new EcmException("程序集目录不存在:" + dir);
        }
        File[] assemblies = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        for (File f : assemblies) {
            scanOtherPluginDir(f);
        }
    }

    private void scanOtherPluginDir(File dir) {
        File[] assemblies = dir.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });
        if (assemblies.length == 0) {
            throw new EcmException("缺少程序集:" + dir);
        }
        if (assemblies.length > 1) {
            throw new EcmException("定义了多个程序集:" + dir);
        }
        String fn = assemblies[0].getAbsolutePath();
        IAssembly pluginAbly = Assembly.loadAssembly(fn);
        pluginAbly.start();
        INodeApplicationPlugin plugin = (INodeApplicationPlugin) pluginAbly.workbin().part("$.cj.studio.node.app.plugin");
        if (plugin == null) {
            throw new EcmException("程序集验证失败，原因：未发现INodeApplicationAuthPlugin 的派生实现,请检查入口服务名：$.cj.studio.node.app.plugin");
        }
        try {
            plugin.onstart(masterNetworkName, pluginSite);
            this.plugins.add(plugin);
        } catch (Exception e) {
            CJSystem.logging().error(this.getClass(), e);
        }
    }

    private void scanAuthPluginAndLoad(String authDir) {
        File dir = new File(authDir);
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
            throw new EcmException("缺少程序集:" + authDir);
        }
        if (assemblies.length > 1) {
            throw new EcmException("定义了多个程序集:" + authDir);
        }
        String fn = assemblies[0].getAbsolutePath();
        IAssembly authAbly = Assembly.loadAssembly(fn);
        authAbly.start();
        this.authPlugin = (INodeApplicationAuthPlugin) authAbly.workbin().part("$.cj.studio.node.app.plugin");
        if (this.authPlugin == null) {
            CJSystem.logging().error(getClass(), "程序集验证失败，原因：未发现INodeApplicationAuthPlugin 的派生实现,请检查入口服务名：$.cj.studio.node.app.plugin");
        } else {
            try {
                authPlugin.onstart(masterNetworkName, pluginSite);
            } catch (Exception e) {
                CJSystem.logging().error(getClass(), e);
            }
        }

    }


    @Override
    public boolean isEnableRBAC() {
        return rbacconfig.isEnableRBAC();
    }

    @Override
    public IAuthenticateStrategy createAuthenticateStrategy(String authMode, INetwork network) {
        if (this.authPlugin != null) {
            IAuthenticateStrategy authenticateStrategy = this.authPlugin.createAuthenticateStrategy(authMode, network);
            if (authenticateStrategy != null) {
                CJSystem.logging().info(getClass(), String.format("发现认证插件的认证策略，已使用"));
                return authenticateStrategy;
            }
            CJSystem.logging().info(getClass(), String.format("认证插件的认证策略为空，已使用系统的认证策略"));
        }
        switch (authMode) {
            case "auth.password":
                return new PasswordAuthenticateStrategy(rbacconfig);
//            case "auth.jwt":
//                return new JwtAuthenticateStrategy();
        }
        return null;
    }

    @Override
    public IAccessControllerStrategy createAccessControllerStrategy() {
        if (this.authPlugin != null) {
            IAccessControllerStrategy accessControllerStrategy = this.authPlugin.createAccessControllerStrategy();
            if (accessControllerStrategy != null) {
                CJSystem.logging().info(getClass(), String.format("发现认证插件的访问控制策略，已使用"));
                return accessControllerStrategy;
            }
            CJSystem.logging().info(getClass(), String.format("应用插件的访问控制策略为空，已使用系统的访问控制策略"));
        }
        return new SystemAccessControllerStrategy(rbacconfig);
    }

    @Override
    public void oninactiveNetwork(INetwork network, IPipeline pipeline) {
        for (INodeApplicationPlugin plugin : this.plugins) {
            try {
                plugin.oninactiveNetwork(network, pipeline);
            } catch (Exception e) {
                CJSystem.logging().error(this.getClass(), e);
                continue;
            }
        }
        for (IValve valve : valves) {
            pipeline.remove(valve);
        }
    }

    @Override
    public void onactivedNetwork(UserPrincipal userPrincipal, INetwork network, IPipeline pipeline) {
        if (masterNetworkName.equals(network.getInfo().getName())) {
            return;
        }
        for (INodeApplicationPlugin plugin : this.plugins) {
            try {
                plugin.onactivedNetwork(userPrincipal, network, pipeline);
            } catch (Exception e) {
                CJSystem.logging().error(this.getClass(), e);
                continue;
            }
        }

        IValve cluster = new ClusterValve(remoteServiceNodeRouter, subscriberContainer.getSubscriberConfig().getBalance(),subscriberContainer.getSubscriberConfig().home());
        this.valves.add(cluster);
        pipeline.append(cluster);
    }

    private class PluginSite implements IServiceProvider {
        public PluginSite(IServiceProvider site) {

        }

        @Override
        public <T> ServiceCollection<T> getServices(Class<T> serviceClazz) {
            return null;
        }

        @Override
        public Object getService(String serviceId) {
            return null;
        }
    }
}
