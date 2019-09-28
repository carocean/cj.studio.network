package cj.studio.network.node;

import cj.studio.network.NetworkInfo;
import cj.studio.network.UserPrincipal;
import cj.studio.util.reactor.IServiceProvider;
import io.netty.channel.Channel;

class DefaultPeerEvent implements IPeerEvent {
    INetworkNodeAppManager app;
    public DefaultPeerEvent(IServiceProvider site) {
        this.app=site.getService("$.network.app.manager");
    }
    @Override
    public void online(String peerName, UserPrincipal userPrincipal, Channel ch, NetworkInfo info) {
        app.onlinePeer(peerName,userPrincipal,ch);
    }

    @Override
    public void offline(String peerName, UserPrincipal userPrincipal, Channel ch, NetworkInfo info) {
        app.offlinePeer(peerName,userPrincipal,ch);
    }


}
