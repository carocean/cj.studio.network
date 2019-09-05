package cj.studio.network.peer;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.ServiceCollection;
import cj.studio.network.NetworkFrame;

class NetworkPeer implements INetworkPeer, IServiceProvider {

    IConnection connection;
    String networkName;
    IOnmessage onmessage;
    IOnopen onopen;
    IOnclose onclose;
    IServiceProvider site;

    public NetworkPeer(IConnection connection, String networkName, IOnopen onopen, IOnmessage onmessage, IOnclose onclose, IServiceProvider site) {
        this.connection = connection;
        this.networkName = networkName;
        this.onmessage = onmessage;
        this.onopen = onopen;
        this.onclose = onclose;
        this.site = site;
    }
    @Override
    public String getNetworkName() {
        return networkName;
    }

    @Override
    public void send(NetworkFrame frame) {
        if (!connection.isConnected()) {
            throw new EcmException("连接未打开");
        }
        String url = String.format("/%s%s", networkName, frame.url());
        frame.url(url);
        connection.send(frame);
    }

    @Override
    public void onrecieve(NetworkFrame frame) {
        if (onopen != null && "NETWORK/1.0".equals(frame.protocol()) && "listenNetwork".equals(frame.command())) {
            onopen.onopen(this);
            return;
        }
        if (onmessage != null) {
            onmessage.onmessage(frame, this);
        }
    }

    @Override
    public void onclose() {
        if (onclose != null) {
            onclose.onclose(this);
        }
    }

    @Override
    public void onopen() {
        if (onopen != null) {
            onopen.onopen(this);
        }
    }

    @Override
    public <T> ServiceCollection<T> getServices(Class<T> serviceClazz) {
        return site.getServices(serviceClazz);
    }

    @Override
    public Object getService(String serviceId) {
        if ("$.network".equals(serviceId)) {
            return this;
        }
        return site.getService(serviceId);
    }
}
