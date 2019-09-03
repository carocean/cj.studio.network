package cj.studio.network.peer;

import cj.studio.ecm.IServiceProvider;

import java.util.HashMap;
import java.util.Map;

public class NetworkPeerContainer implements INetworkPeerContainer {
    Map<String, INetworkPeer> networkPeerMap;
    IServiceProvider site;
    public NetworkPeerContainer(IServiceProvider site) {
        this.networkPeerMap = new HashMap<>();
        this.site=site;
    }

    @Override
    public boolean exists(String networkName) {
        return networkPeerMap.containsKey(networkName);
    }

    @Override
    public INetworkPeer create(IConnection connection, String networkName, IOnmessage onmessage) {
        INetworkPeer networkPeer = new NetworkPeer(connection, networkName, onmessage,site);
        networkPeerMap.put(networkName, networkPeer);
        return networkPeer;
    }

    @Override
    public void dispose() {
        networkPeerMap.clear();
    }

    @Override
    public INetworkPeer get(String networkName) {
        return networkPeerMap.get(networkName);
    }
}
