package cj.studio.network.nodeapp;

import cj.studio.ecm.*;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.annotation.CjService;
import cj.studio.network.*;
import cj.studio.network.nodeapp.subscriber.*;
import cj.studio.network.nodeapp.strategy.PasswordAuthenticateStrategy;
import cj.studio.network.nodeapp.strategy.SystemAccessControllerStrategy;
import cj.studio.network.INodeApplicationPlugin;
import cj.studio.network.nodeapp.subscriber.ICluster;
import cj.studio.util.reactor.*;
import io.netty.channel.Channel;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CjService(name = "$.cj.studio.node.app", isExoteric = true)
public class NodeApplication implements INodeApplication {
    IRBACConfig rbacconfig;
    IPluginConfig pluginConfig;
    String masterNetworkName;
    List<IValve> valves;
    ISubscriberContainer subscriberContainer;
    INodeApplicationAuthPlugin authPlugin;
    Map<String, INodeApplicationPlugin> plugins;//key是插件的uuid
    IServiceProvider pluginSite;
    ICluster cluster;
    IRemoteNodeBalancer remoteNodeBalancer;
    @Override
    public  void onstart(String home, String masterNetworkName, IServiceProvider site) {
        pluginSite = new PluginSite(site);
        this.masterNetworkName = masterNetworkName;
        this.valves = new ArrayList<>();
        rbacconfig = new RBACConfig(masterNetworkName);
        try {
            rbacconfig.load(home);
        } catch (FileNotFoundException e) {
            throw new EcmException(e);
        }
        pluginConfig = new PluginConfig();
        try {
            pluginConfig.load(home);
        } catch (FileNotFoundException e) {
            throw new EcmException(e);
        }
        cluster = new DefaultCluster();
        remoteNodeBalancer =new DefaultRemoteNodeBalancer(cluster);
        subscriberContainer = new SubscriberContainer(cluster);
        subscriberContainer.start(home, site);

        pluginSite = new PluginSite(site);
        scanPluginsAndLoad(home);
    }

    private void scanPluginsAndLoad(String home) {
        //先加载认证插件
        String authDir = String.format("%s%splugins%sauth", home, File.separator, File.separator);
        scanAuthPluginAndLoad(authDir);

        //再加载其它插件
        plugins = new HashMap<>();
        String othersDir = String.format("%s%splugins%sothers", home, File.separator, File.separator);
        scanOtherPluginsAndLoad(othersDir);

    }

    private void scanOtherPluginsAndLoad(String othersDir) {
        File dir = new File(othersDir);
        if (!dir.exists()) {
            return;
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
        if (this.plugins.containsKey(pluginAbly.info().getName())) {
            throw new EcmException("插件装载失败，已存在同名插件：" + pluginAbly.info().getName());
        }
        INodeApplicationPlugin plugin = (INodeApplicationPlugin) pluginAbly.workbin().part("$.cj.studio.node.app.plugin");
        if (plugin == null) {
            throw new EcmException("程序集验证失败，原因：未发现INodeApplicationAuthPlugin 的派生实现,请检查入口服务名：$.cj.studio.node.app.plugin");
        }
        try {
            plugin.onstart(masterNetworkName, pluginSite);
            this.plugins.put(pluginAbly.info().getName(), plugin);
            CJSystem.logging().info(getClass(), String.format("成功装载第三方插件:%s，是否已配置为失活:%s", pluginAbly.info().getName(), pluginConfig.containsDisableOthers(pluginAbly.info().getName())));
        } catch (Exception e) {
            CJSystem.logging().error(this.getClass(), e);
        }
    }

    private void scanAuthPluginAndLoad(String authDir) {
        File dir = new File(authDir);
        if (!dir.exists()) {
           return;
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
                authPlugin.onstart(authDir,masterNetworkName, pluginSite);
                CJSystem.logging().info(getClass(), String.format("成功装载Auth插件。是否已配置为失活：%s",pluginConfig.isDisableAuth()));
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
    public synchronized IAuthenticateStrategy createAuthenticateStrategy(String authMode, INetwork network) {
        if (this.authPlugin != null && !this.pluginConfig.isDisableAuth()) {
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
    public synchronized IAccessControllerStrategy createAccessControllerStrategy() {
        if (this.authPlugin != null && !this.pluginConfig.isDisableAuth()) {
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
    public synchronized void onlinePeer(String peerName, UserPrincipal userPrincipal, Channel source,INetwork network) {
        for (Map.Entry<String, INodeApplicationPlugin> entry : this.plugins.entrySet()) {
            INodeApplicationPlugin plugin = entry.getValue();
            if(plugin==null)continue;
            if (this.pluginConfig.containsDisableOthers(entry.getKey())) {
                continue;
            }
            try {
                plugin.onlinePeer(peerName, userPrincipal, source,network,this.remoteNodeBalancer);
            } catch (Exception e) {
                CJSystem.logging().error(this.getClass(), e);
                continue;
            }
        }
    }

    @Override
    public synchronized void offlinePeer(String peerName, UserPrincipal userPrincipal, Channel source,INetwork network) {
        for (Map.Entry<String, INodeApplicationPlugin> entry : this.plugins.entrySet()) {
            INodeApplicationPlugin plugin = entry.getValue();
            if(plugin==null)continue;
            if (this.pluginConfig.containsDisableOthers(entry.getKey())) {
                continue;
            }
            try {
                plugin.offlinePeer(peerName, userPrincipal, source,network,this.remoteNodeBalancer);
            } catch (Exception e) {
                CJSystem.logging().error(this.getClass(), e);
                continue;
            }
        }
    }

    @Override
    public synchronized void oninactiveNetwork(INetwork network, IPipeline pipeline) {
        for (Map.Entry<String, INodeApplicationPlugin> entry : this.plugins.entrySet()) {
            INodeApplicationPlugin plugin = entry.getValue();
            if(plugin==null)continue;
            if (this.pluginConfig.containsDisableOthers(entry.getKey())) {
                continue;
            }
            try {
                plugin.oninactiveNetwork(network, pipeline,this.remoteNodeBalancer);
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
    public synchronized void onactivedNetwork(UserPrincipal userPrincipal, INetwork network, IPipeline pipeline) {
        if (masterNetworkName.equals(network.getInfo().getName())) {
            return;
        }
        for (Map.Entry<String, INodeApplicationPlugin> entry : this.plugins.entrySet()) {
            INodeApplicationPlugin plugin = entry.getValue();
            if(plugin==null)continue;
            if (this.pluginConfig.containsDisableOthers(entry.getKey())) {
                continue;
            }
            try {
                plugin.onactivedNetwork(userPrincipal, network, pipeline,this.remoteNodeBalancer);
            } catch (Exception e) {
                CJSystem.logging().error(this.getClass(), e);
                continue;
            }
        }

        IValve clusterValve = new ClusterValve(this.cluster);
        this.valves.add(clusterValve);
        pipeline.append(clusterValve);
    }

    private class PluginSite implements IServiceProvider {
        IServiceProvider site;

        public PluginSite(IServiceProvider site) {
            this.site = site;
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
