package cj.studio.network.peer.connection;

import cj.studio.network.peer.IOnclose;
import cj.studio.network.peer.IOnerror;
import cj.studio.network.peer.IOnmessage;
import cj.studio.network.peer.IOnopen;

public interface IOnReconnectEvent {
    void onreconnect();

    void init(String authmode, String user, String token, IOnerror onerror, IOnopen onopen, IOnmessage onmessage, IOnclose onclose);

}
