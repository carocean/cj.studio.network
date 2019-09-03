package cj.studio.network.peer;

import cj.ultimate.IDisposable;

public interface INetworkPeerContainer extends IDisposable {

    boolean exists(String networkName);

    INetworkPeer create(IConnection connection, String networkName, IOnmessage onmessage);

    INetworkPeer get(String networkName);

}
