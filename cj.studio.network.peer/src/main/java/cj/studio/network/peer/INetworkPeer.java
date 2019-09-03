package cj.studio.network.peer;


import cj.studio.network.NetworkFrame;

public interface INetworkPeer {
    void send(NetworkFrame frame);

    void recieve(NetworkFrame frame);

}
