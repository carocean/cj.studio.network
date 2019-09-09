package cj.studio.network.peer;

import cj.studio.ecm.IServiceProvider;
import cj.studio.network.peer.connection.IOnReconnectEvent;

public interface IPeer {
    void connect(String networkNodeAddress, String masterNetowrkName, IOnReconnectEvent onReconnectEvent);

    void connect(String networkNodeAddress, String masterNetowrkName);
    public IMasterNetworkPeer auth(String authmode, String user, String token,IOnerror onerror, IOnopen onopen, IOnmessage onmessage, IOnclose onclose);
    INetworkPeer listen(String networkName, IOnerror onerror,IOnopen onopen, IOnmessage onmessage, IOnclose onclose);

    void close();

    String peerName();

    String getNodeHost();

    String getNodeProtocol();

    int getNodePort();

    IServiceProvider site();

}

