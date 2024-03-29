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
    public String getMasterNetowrkName() {
        return masterNetowrkName;
    }
    @Override
    public INetworkPeer getMasterNetwork(){
        return networkPeerMap.get(masterNetowrkName);
    }
    @Override
    public boolean exists(String networkName) {
        return networkPeerMap.containsKey(networkName);
    }


    @Override
    public INetworkPeer create(IConnection connection, String networkName,IOnerror onerror, IOnopen onopen, IOnmessage onmessage, IOnclose onclose) {
        INetworkPeer networkPeer = null;
        if(masterNetowrkName.equals(networkName)){
            networkPeer = new MasterNetworkPeer(connection, networkName,onerror, onopen, onmessage, onclose, site);
        }else {
            networkPeer = new NetworkPeer(connection, networkName,onerror, onopen, onmessage, onclose, site);
        }
        networkPeerMap.put(networkName, networkPeer);
        return networkPeer;
    }

    @Override
    public String[] enumNetworkName() {
        return this.networkPeerMap.keySet().toArray( new String[0]);
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
    public void remove(INetworkPeer networkPeer) {
        networkPeerMap.remove(networkPeer.getNetworkName());
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
