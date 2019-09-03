package cj.studio.network.node;

import cj.studio.ecm.IServiceProvider;
import io.netty.channel.Channel;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkContainer implements INetworkContainer {
    IServiceProvider site;
    Map<String, INetwork> networks;
    private NetworkInfo managerNetworkInfo;

    public NetworkContainer(IServiceProvider site) {
        this.site = site;
        INetworkNodeConfig config = (INetworkNodeConfig) site.getService("$.network.config");
        init(config);
    }

    protected void init(INetworkNodeConfig config) {
        networks = new ConcurrentHashMap<>();
        this.managerNetworkInfo = config.getManagerNetwork();
        //建立主网络
        INetwork managerNW = new Network(config.getManagerNetwork());
        networks.put(managerNW.getInfo().getName(), managerNW);
        for (Map.Entry<String, NetworkInfo> entry : config.getGeneralNetworks().entrySet()) {
            NetworkInfo info = entry.getValue();
            INetwork network = new Network(info);
            networks.put(info.getName(), network);
        }
    }

    @Override
    public String[] enumNetworkName(boolean isSorted) {
        String[] names = networks.keySet().toArray(new String[0]);
        if (!isSorted) {
            return names;
        }
        Arrays.sort(names);
        return names;
    }

    @Override
    public INetwork createNetwork(String name, String castmode) {
        NetworkInfo info = new NetworkInfo(name, castmode);
        INetwork managerNW = new Network(info);
        networks.put(managerNW.getInfo().getName(), managerNW);
        return managerNW;
    }

    @Override
    public void renameNetwork(String networkName, String newNetworkName) {
        INetwork network = networks.get(networkName);
        if (network == null) return;
        NetworkInfo info = network.getInfo();
        info.setName(newNetworkName);
        networks.put(networkName,network);
    }

    @Override
    public void changeNetworkCastmode(String networkName, String castmode) {
        INetwork network = networks.get(networkName);
        if (network == null) return;
        network.getInfo().setCastmode(castmode);
    }

    @Override
    public boolean existsNetwork(String networkName) {
        return networks.containsKey(networkName);
    }

    @Override
    public NetworkInfo getManagerNetworkInfo() {
        return managerNetworkInfo;
    }

    @Override
    public INetwork getManagerNetwork() {
        return this.networks.get(this.managerNetworkInfo.getName());
    }

    @Override
    public void onChannelInactive(Channel channel) {
        //如果频繁关闭会占用服务器工作线程较长时间，是否放到专属线程清理之后再说吧
        for (Map.Entry<String, INetwork> entry : networks.entrySet()) {
            INetwork network = entry.getValue();
            if (network == null) {
                continue;
            }
            network.removeChannel(channel);
        }
    }

    @Override
    public void removeNetwork(String networkName) {
        INetwork network = networks.get(networkName);
        if (network == null) {
            return;
        }
        network.dispose();
        networks.remove(networkName);
    }

    @Override
    public INetwork getNetwork(String networkName) {
        return networks.get(networkName);
    }


}
