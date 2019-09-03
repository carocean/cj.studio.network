package cj.studio.network.test;

import cj.studio.ecm.IServiceProvider;
import cj.studio.network.Circuit;
import cj.studio.network.Frame;
import cj.studio.network.peer.INetworkPeer;
import cj.studio.network.peer.IOnmessage;
import cj.studio.network.peer.IPeer;
import cj.studio.network.peer.Peer;

public class TestPeer {
    public static void main(String... args) throws InterruptedException {
        IPeer peer = Peer.create("mypeer",null);
        INetworkPeer manager = peer.connect("tcp://localhost:6600?workThreadCount=8", "auth.password", "cj", "11", "manager-network", new IOnmessage() {
            @Override
            public void onmessage(Frame frame, IServiceProvider site) {
               byte[] b= frame.content().readFully();
                Circuit circuit=new Circuit(b);
                StringBuffer sb=new StringBuffer();
                circuit.print(sb);
                System.out.println("------"+frame+"\r\n"+sb);
                if("auth".equals(frame.command())&&circuit.status().equals("200")){
                    work(site);
                }
            }
        });//

        Thread.sleep(30000L);
    }

    private static void work(IServiceProvider site) {
        INetworkPeer networkPeer=(INetworkPeer)site.getService("$.network");
        Frame frame = new Frame("listNetwork / network/1.0");
        networkPeer.send(frame);
//
//
//        INetworkPeer np1 = peer.listen("network-1", new IOnmessage() {
//            @Override
//            public void onmessage(Frame frame) {
//
//            }
//        });
//        Frame frame2 = new Frame("get /yy/ fx/1.0");
//        np1.send(frame2);
//
//        INetworkPeer np2 = peer.listen("network-2", new IOnmessage() {
//            @Override
//            public void onmessage(Frame frame) {
//
//            }
//        });
//        np2.send(frame);


//        peer.close();
    }
}
