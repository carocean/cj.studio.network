package cj.studio.network.console;

import cj.studio.ecm.IServiceProvider;
import cj.studio.network.NetworkCircuit;
import cj.studio.network.NetworkFrame;
import cj.studio.network.peer.INetworkPeer;
import cj.studio.network.peer.IOnmessage;
import cj.studio.network.peer.IPeer;
import cj.studio.network.peer.Peer;
import cj.ultimate.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.cli.*;
import org.apache.log4j.BasicConfigurator;

import java.util.UUID;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class PeerConsole {

    public static void main(String... args) throws InterruptedException, ParseException {
        String fileName = "cj.studio.network.console";
        Options options = new Options();

        Option n = new Option("n", "peername", true, "[可省略，默认以guid生成]本地peer名");
        options.addOption(n);
        Option r = new Option("r", "url", true, "[必须]远程node的url地址，格式：protocol://host:port?workThreadCount=2&prop2=yy");
        options.addOption(r);
        Option g = new Option("g", "mnn", true, "[必须]管理网络名字");
        options.addOption(g);
        Option a = new Option("a", "authmode", true, "[可省略，默认为auth.password]认证方式，包括：auth.password,auth.jwt");
        options.addOption(a);
        Option u = new Option("u", "user", true, "[必须]用户名");
        options.addOption(u);
        Option p = new Option("p", "pwd", true, "[必须]密码或令牌可为空");
        options.addOption(p);

        Option m = new Option("m", "man", false, "帮助");
        options.addOption(m);
        Option debug = new Option("d", "debug", true, "调试命令行程序集时使用，需指定以下jar包所在目录\r\n" + fileName);
        options.addOption(debug);
        // GnuParser
        // BasicParser
        // PosixParser
        GnuParser parser = new GnuParser();
        CommandLine line = parser.parse(options, args);

        if (line.hasOption("m")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("network node", options);
            return;
        }
        if (!line.hasOption("r") || !line.hasOption("g") || !line.hasOption("u") || !line.hasOption("p")) {
            System.out.println("缺少必须参数，请使用-m参数查看帮助");
            return;
        }
        String peername = line.getOptionValue("n");
        if (StringUtil.isEmpty(peername)) {
            peername = UUID.randomUUID().toString();
        }
        String url = line.getOptionValue("r");
        String authmode = line.getOptionValue("a");
        if (StringUtil.isEmpty(authmode)) {
            authmode = "auth.password";
        }
        String user = line.getOptionValue("u");
        String pwd = line.getOptionValue("p");
        String managerNetwork = line.getOptionValue("g");

        IPeer peer = Peer.create(peername, null);
        //"tcp://localhost:6600?workThreadCount=8"
        ReentrantLock lock = new ReentrantLock();
        Condition finished = lock.newCondition();
        INetworkPeer manager = peer.connect(url, authmode, user, pwd, managerNetwork, new IOnmessage() {
            @Override
            public void onmessage(NetworkFrame frame, IServiceProvider site) {
                byte[] b = frame.content().readFully();
                NetworkCircuit circuit = new NetworkCircuit(b);
//                StringBuffer sb = new StringBuffer();
//                circuit.print(sb);
//                System.out.println("------" + frame + "\r\n" + sb);
                if ("auth".equals(frame.command()) && circuit.status().equals("200")) {
                    //登录成功后开始工作
                    System.out.println(String.format("Peer:%s 已进入节点:%s://%s:%s，让我们开始工作吧!",peer.peerName(),peer.getNodeProtocol(),peer.getNodeHost(),peer.getNodePort()));
                    moniter(site);
                    try{
                        lock.lock();
                        finished.signalAll();
                    }finally {
                        lock.unlock();
                    }

                }
            }
        });//
        try {
            lock.lock();
            finished.await();
            System.out.println("已退出");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }finally {
            lock.unlock();
        }

    }

    private static void moniter(IServiceProvider site) {
        IPeerMonitor console = new PeerMonitor();
        console.moniter(site);
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
        ByteBuf bb = Unpooled.buffer();
        bb.writeBytes("我来了".getBytes());
        NetworkFrame frame2 = new NetworkFrame("get /yy/?type=23 fx/1.0", bb);
        frame2.head("my", "ss");
        np1.send(frame2);

        //上面用了network-1，再查一下各网络中的peer如何
        networkPeer.send(frame);
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
