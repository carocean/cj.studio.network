package cj.studio.network.console;

import cj.studio.network.NetworkFrame;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class TestNode {
    public static void main(String... args) throws InterruptedException {
        TestCreateGeneralNetworkByManagerNetwork();
        Thread.currentThread().sleep(3000L);
        TestInfoGeneralNetworkByManagerNetwork();
        TestListGeneralNetworkByManagerNetwork();
        TestExistsGeneralNetworkByManagerNetwork();
        TestRemoveGeneralNetworkByManagerNetwork();
        TestListGeneralNetworkByManagerNetwork();
//        TestSendByGeneralNetwork();//一定要在前面创建成功后再发送信息，因为是异步的
        Thread.currentThread().sleep(Integer.MAX_VALUE);
    }

    public static void TestCreateGeneralNetworkByManagerNetwork() {
        TcpClient client = new TcpClient();
        Sender sender = client.connect("localhost", 6600);
        ByteBuf bb = Unpooled.buffer();
        bb.writeBytes("这是主网络".getBytes());
        NetworkFrame frame = new NetworkFrame("createNetwork /manager-network network/1.0", bb);
        frame.head("Network-Name", "network-3");
        frame.head("Network-Castmode", "multicast");
        sender.send(frame);
    }
    public static void TestInfoGeneralNetworkByManagerNetwork() {
        TcpClient client = new TcpClient();
        Sender sender = client.connect("localhost", 6600);
        ByteBuf bb = Unpooled.buffer();
        bb.writeBytes("查network-1的信息".getBytes());
        NetworkFrame frame = new NetworkFrame("infoNetwork /manager-network network/1.0", bb);
        frame.head("Network-Name", "network-1");
        sender.send(frame);
    }
    public static void TestRemoveGeneralNetworkByManagerNetwork() {
        TcpClient client = new TcpClient();
        Sender sender = client.connect("localhost", 6600);
        ByteBuf bb = Unpooled.buffer();
        bb.writeBytes("这是主网络".getBytes());
        NetworkFrame frame = new NetworkFrame("removeNetwork /manager-network network/1.0", bb);
        frame.head("Network-Name", "network-3");
        sender.send(frame);
    }
    public static void TestListGeneralNetworkByManagerNetwork() {
        TcpClient client = new TcpClient();
        Sender sender = client.connect("localhost", 6600);
        ByteBuf bb = Unpooled.buffer();
        bb.writeBytes("这是主网络".getBytes());
        NetworkFrame frame = new NetworkFrame("listNetwork /manager-network network/1.0", bb);
        sender.send(frame);
    }
    public static void TestExistsGeneralNetworkByManagerNetwork() {
        TcpClient client = new TcpClient();
        Sender sender = client.connect("localhost", 6600);
        ByteBuf bb = Unpooled.buffer();
        bb.writeBytes("这是主网络".getBytes());
        NetworkFrame frame = new NetworkFrame("existsNetwork /manager-network/ network/1.0", bb);
        frame.head("Network-Name","network-3");
        sender.send(frame);
    }
    public static void TestSendByGeneralNetwork() {
        TcpClient client = new TcpClient();
        Sender sender = client.connect("localhost", 6600);
        ByteBuf bb = Unpooled.buffer();
        bb.writeBytes("这是一般网络".getBytes());
        NetworkFrame frame = new NetworkFrame("get /network-2 xx/1.0", bb);
        sender.send(frame);
    }
}
