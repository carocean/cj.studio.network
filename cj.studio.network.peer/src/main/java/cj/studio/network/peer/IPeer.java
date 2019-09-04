package cj.studio.network.peer;

public interface IPeer {
    INetworkPeer connect(String networkNode,String authmode, String user, String token,String managerNetowrkName, IOnmessage onmessage);

    INetworkPeer listen(String networkName, IOnmessage onmessage);

    void close();
    String peerName();
    String getNodeHost();

    String getNodeProtocol();

    int getNodePort();
}

