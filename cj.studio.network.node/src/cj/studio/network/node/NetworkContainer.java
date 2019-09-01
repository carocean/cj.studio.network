package cj.studio.network.node;

import cj.studio.ecm.IServiceProvider;

public class NetworkContainer implements INetworkContainer {
    @Override
    public void refresh() {

    }

    public NetworkContainer(IServiceProvider site) {

    }

    @Override
    public boolean existsNetwork(String network) {
        return false;
    }

    @Override
    public NetworkInfo getManagerNetworkInfo() {
        return null;
    }
}
