package cj.studio.network.peer;


import cj.studio.ecm.IServiceProvider;
import cj.studio.network.NetworkFrame;

public interface INetworkPeer {
    String getNetworkName();

    void send(NetworkFrame frame);

    void onrecieve(NetworkFrame frame);

    void onclose();
    IServiceProvider site();
}
