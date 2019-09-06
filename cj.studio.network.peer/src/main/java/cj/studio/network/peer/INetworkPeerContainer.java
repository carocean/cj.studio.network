package cj.studio.network.peer;

import cj.ultimate.IDisposable;
import io.netty.channel.ChannelHandlerContext;

public interface INetworkPeerContainer extends IDisposable {

    String getMasterNetowrkName();

    INetworkPeer getMasterNetwork();

    boolean exists(String networkName);

    INetworkPeer create(IConnection connection, String networkName,IOnerror onerror, IOnopen onopen, IOnmessage onmessage,IOnclose onclose);

    INetworkPeer get(String networkName);


    void onclose();

    void remove(INetworkPeer networkPeer);
}
