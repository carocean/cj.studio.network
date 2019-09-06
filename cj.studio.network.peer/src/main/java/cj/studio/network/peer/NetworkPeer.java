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
    IOnerror onerror;
    IServiceProvider site;

    public NetworkPeer(IConnection connection, String networkName,IOnerror onerror, IOnopen onopen, IOnmessage onmessage, IOnclose onclose, IServiceProvider site) {
        this.connection = connection;
        this.networkName = networkName;
        this.onmessage = onmessage;
        this.onopen = onopen;
        this.onclose = onclose;
        this.onerror=onerror;
        this.site = site;
    }

    @Override
    public IServiceProvider site() {
        return this;
    }

    @Override
    public String getNetworkName() {
        return networkName;
    }

    @Override
    public void info() {
        NetworkFrame frame=new NetworkFrame("infoNetwork / network/1.0");
        frame.head("Network-Name",getNetworkName());
        this.send(frame);//查网络信息是主网络命令
    }

    @Override
    public void bye() {
        NetworkFrame frame=new NetworkFrame("byeNetwork / network/1.0");
        this.send(frame);
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
        if("NETWORK/1.0".equals(frame.protocol())){
            if (onopen != null&&"listenNetwork".equals(frame.command()) ) {
                onopen.onopen(frame,this);
                return;
            }
            if (onerror != null&&"error".equals(frame.command()) ) {
                onerror.onerror(frame,this);
                return;
            }
            if (onclose != null&&"byeNetwork".equals(frame.command()) ) {
                INetworkPeerContainer container=(INetworkPeerContainer) site.getService("$.peer.container");
                container.remove(this);
                onclose.onclose(this);
                return;
            }
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
    public <T> ServiceCollection<T> getServices(Class<T> serviceClazz) {
        return site.getServices(serviceClazz);
    }

    @Override
    public Object getService(String serviceId) {
        if ("$.current".equals(serviceId)) {
            return this;
        }
        return site.getService(serviceId);
    }
}
