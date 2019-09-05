package cj.studio.network.peer;

import cj.studio.ecm.IServiceProvider;

public interface IPeer {
    INetworkPeer connect(String networkNode,String authmode, String user, String token,String masterNetowrkName,IOnopen onopen, IOnmessage onmessage,IOnclose onclose);

    INetworkPeer listen(String networkName, IOnopen onopen, IOnmessage onmessage,IOnclose onclose);

    void close();
    String peerName();
    String getNodeHost();

    String getNodeProtocol();

    int getNodePort();

    IServiceProvider site();

}

