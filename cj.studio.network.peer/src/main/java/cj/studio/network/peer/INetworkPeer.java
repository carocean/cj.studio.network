package cj.studio.network.peer;


import cj.studio.network.Frame;

public interface INetworkPeer {
    void send(Frame frame);

    void recieve(Frame frame);

}
