package cj.studio.network.node;

import cj.studio.network.INetwork;
import cj.studio.network.NetworkInfo;
import cj.studio.network.UserPrincipal;
import io.netty.channel.Channel;

public interface IPeerEvent {
    void online(String peerName, UserPrincipal userPrincipal, Channel source, INetwork network);

    void offline(String peerName, UserPrincipal userPrincipal, Channel source,INetwork network);

}
