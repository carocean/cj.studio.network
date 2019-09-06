package cj.studio.network.console.test;

import cj.studio.ecm.IServiceProvider;
import cj.studio.network.NetworkFrame;
import cj.studio.network.peer.*;

public class OnMasterEvent implements IOnopen, IOnerror, IOnclose, IOnmessage {
    @Override
    public void onclose(INetworkPeer networkPeer) {
        System.out.println("----OnMasterEvent.onclose");
    }

    @Override
    public void onerror(NetworkFrame frame, INetworkPeer networkPeer) {
        System.out.println("----OnMasterEvent.onerror");
    }

    @Override
    public void onmessage(NetworkFrame frame, IServiceProvider site) {
        System.out.println("----OnMasterEvent.onmessage");
    }

    @Override
    public void onopen(NetworkFrame frame, INetworkPeer networkPeer) {
        System.out.println("----OnMasterEvent.onopen");
    }
}
