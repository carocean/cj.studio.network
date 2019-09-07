package cj.studio.network.console.test;

import cj.studio.network.NetworkFrame;
import cj.studio.network.peer.INetworkPeer;
import cj.studio.network.peer.IPeer;
import cj.studio.network.peer.Peer;

public class TestPeer {
    static IPeer peer;

    public static void main(String... args) throws InterruptedException {
        peer = Peer.create("mypeer", null);
        OnMasterEvent onEvent = new OnMasterEvent();
        peer.connect("tcp://localhost:6600?workThreadCount=8&heartbeat=5000", "master-network");
        INetworkPeer networkPeer=peer.auth("cj", "11", "11", onEvent, onEvent, onEvent, onEvent);
                TestCreateGeneralNetworkByManagerNetwork();
        Thread.sleep(Integer.MAX_VALUE);
    }

    public static void TestCreateGeneralNetworkByManagerNetwork() throws InterruptedException {
        OnWorksEvent onWorksEvent = new OnWorksEvent();
        INetworkPeer networkPeer=peer.listen("network-2", onWorksEvent, onWorksEvent, onWorksEvent, onWorksEvent);
        for(int i=0;i<Integer.MAX_VALUE;i++) {
            networkPeer.send(new NetworkFrame(String.format("get /xxx-%s xx/1.0",i)));
            Thread.sleep(1000L);
        }
    }

}
