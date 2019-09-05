package cj.studio.network.peer;

import cj.studio.network.NetworkFrame;

public interface IOnerror {
    void onerror(NetworkFrame frame,INetworkPeer networkPeer);
}
