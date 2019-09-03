package cj.studio.network.test;

import cj.studio.ecm.IServiceProvider;
import cj.studio.network.NetworkCircuit;
import cj.studio.network.NetworkFrame;
import cj.studio.network.peer.INetworkPeer;
import cj.studio.network.peer.IOnmessage;
import cj.studio.network.peer.IPeer;
import cj.studio.network.peer.Peer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class TestPeer {
    public static void main(String... args) throws InterruptedException {
        IPeer peer = Peer.create("mypeer", null);
        INetworkPeer manager = peer.connect("tcp://localhost:6600?workThreadCount=8", "auth.password", "cj", "11", "manager-network", new IOnmessage() {
            @Override
            public void onmessage(NetworkFrame frame, IServiceProvider site) {
                byte[] b = frame.content().readFully();
                NetworkCircuit circuit = new NetworkCircuit(b);
                StringBuffer sb = new StringBuffer();
                circuit.print(sb);
                System.out.println("------" + frame + "\r\n" + sb);
                if ("auth".equals(frame.command()) && circuit.status().equals("200")) {
                    work(site);//登录成功后开始工作
                }
            }
        });//

        Thread.sleep(30000L);
    }

    private static void work(IServiceProvider site) {
        INetworkPeer networkPeer = (INetworkPeer) site.getService("$.network");
        NetworkFrame frame = new NetworkFrame("listNetwork / network/1.0");
        networkPeer.send(frame);
//
        IPeer peer = (IPeer) site.getService("$.peer");
        INetworkPeer np1 = peer.listen("network-1", new IOnmessage() {
            @Override
            public void onmessage(NetworkFrame frame, IServiceProvider site) {
                StringBuffer sb = new StringBuffer();
                frame.print(sb);
                System.out.println("---network-1---");
                System.out.println(sb);

            }
        });
        ByteBuf bb= Unpooled.buffer();
        bb.writeBytes("我来了".getBytes());
        NetworkFrame frame2 = new NetworkFrame("get /yy/?type=23 fx/1.0",bb);
        frame2.head("my","ss");
        np1.send(frame2);
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
