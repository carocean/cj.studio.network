package cj.studio.network.peer;

import cj.studio.network.NetworkCircuit;
import cj.studio.network.NetworkFrame;

public interface IOnerror {

    void onerror(NetworkFrame frame, NetworkCircuit circuit, INetworkPeer networkPeer);
}
