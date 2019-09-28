package cj.studio.network.node;

import cj.studio.network.NetworkInfo;
import cj.studio.network.UserPrincipal;
import io.netty.channel.Channel;

public interface IPeerEvent {
    void online(String peerName, UserPrincipal userPrincipal, Channel ch, NetworkInfo info);

    void offline(String peerName, UserPrincipal userPrincipal, Channel ch, NetworkInfo info);

}
