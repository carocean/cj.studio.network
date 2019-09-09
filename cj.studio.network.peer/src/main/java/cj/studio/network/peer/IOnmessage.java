package cj.studio.network.peer;

import cj.studio.network.NetworkFrame;

public interface IOnmessage {
    void onmessage(NetworkFrame frame, INetworkPeer networkPeer);
}
