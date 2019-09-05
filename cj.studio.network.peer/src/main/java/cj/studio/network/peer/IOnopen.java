package cj.studio.network.peer;

import cj.studio.network.NetworkFrame;

public interface IOnopen {
    void onopen(NetworkFrame frame,INetworkPeer networkPeer);
}
