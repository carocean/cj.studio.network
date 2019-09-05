package cj.studio.network.peer;

import cj.studio.ecm.IServiceProvider;

import java.util.HashMap;
import java.util.Map;

public class NetworkPeerContainer implements INetworkPeerContainer {
    private String masterNetowrkName;
    Map<String, INetworkPeer> networkPeerMap;
    IServiceProvider site;


    public NetworkPeerContainer(String masterNetowrkName, IServiceProvider site) {
        this.networkPeerMap = new HashMap<>();
        this.site = site;
        this.masterNetowrkName = masterNetowrkName;
    }

    @Override
    public boolean exists(String networkName) {
        return networkPeerMap.containsKey(networkName);
    }

    @Override
    public INetworkPeer create(IConnection connection, String networkName,IOnerror onerror, IOnopen onopen, IOnmessage onmessage, IOnclose onclose) {
        INetworkPeer networkPeer = new NetworkPeer(connection, networkName,onerror, onopen, onmessage, onclose, site);
        networkPeerMap.put(networkName, networkPeer);
        return networkPeer;
    }

    @Override
    public void onclose() {
        for (Map.Entry<String, INetworkPeer> entry : networkPeerMap.entrySet()) {
            if (entry.getKey().equals(this.masterNetowrkName)) {
                continue;
            }
            entry.getValue().onclose();
        }
        networkPeerMap.get(masterNetowrkName).onclose();
        networkPeerMap.clear();
    }

    @Override
    public void dispose() {
        onclose();
    }

    @Override
    public INetworkPeer get(String networkName) {
        return networkPeerMap.get(networkName);
    }
}
