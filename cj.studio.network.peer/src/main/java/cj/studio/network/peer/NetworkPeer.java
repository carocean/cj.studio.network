package cj.studio.network.peer;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.ServiceCollection;
import cj.studio.network.Frame;

class NetworkPeer implements INetworkPeer,IServiceProvider {

    IConnection connection;
    String networkName;
    IOnmessage onmessage;
    IServiceProvider site;
    public NetworkPeer(IConnection connection, String networkName, IOnmessage onmessage,IServiceProvider site) {
        this.connection = connection;
        this.networkName = networkName;
        this.onmessage = onmessage;
        this.site=site;
    }

    @Override
    public void send(Frame frame) {
        if (!connection.isConnected()) {
            throw new EcmException("连接未打开");
        }
        String url=String.format("/%s%s",networkName,frame.url());
        frame.url(url);
        connection.send(frame);
    }

    @Override
    public void recieve(Frame frame) {
        if (onmessage != null) {
            onmessage.onmessage(frame,this);
        }
    }

    @Override
    public <T> ServiceCollection<T> getServices(Class<T> serviceClazz) {
        return site.getServices(serviceClazz);
    }

    @Override
    public Object getService(String serviceId) {
        if("$.network".equals(serviceId)){
            return this;
        }
        return site.getService(serviceId);
    }
}
