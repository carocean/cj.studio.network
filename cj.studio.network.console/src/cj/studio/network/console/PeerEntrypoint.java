package cj.studio.network.console;

import cj.studio.ecm.IServiceProvider;
import cj.studio.network.NetworkCircuit;
import cj.studio.network.NetworkFrame;
import cj.studio.network.peer.*;
import cj.ultimate.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.cli.*;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class PeerEntrypoint {

    public static void main(String... args) throws ParseException, IOException {
        String fileName = "cj.studio.network.console";
        Options options = new Options();

        Option n = new Option("n", "peername", true, "[可省略，默认以guid生成]本地peer名");
        options.addOption(n);
        Option r = new Option("r", "url", true, "[必须]远程node的url地址，格式：'protocol://host:port?workThreadCount=2&prop2=yy'。注意：如果含有&符则必须加单引号将整个url包住");
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

        File consoleFile = getHomeDir(fileName, line);
        PropertyConfigurator.configure(String.format("%s%sconf%slog4j.properties", consoleFile.getParent(), File.separator, File.separator));

        IPeer peer = Peer.create(peername, null);
        //"tcp://localhost:6600?workThreadCount=8"
        ReentrantLock lock = new ReentrantLock();
        Condition finished = lock.newCondition();
        ChildMonitorController childMonitorController=new ChildMonitorController();
        INetworkPeer manager = peer.connect(url, authmode, user, pwd, managerNetwork, new IOnerror() {
            @Override
            public void onerror(NetworkFrame frame, INetworkPeer networkPeer) {
                byte[] b = frame.content().readFully();
                NetworkCircuit circuit = new NetworkCircuit(b);
                if("404".equals(circuit.status())){
                    b=circuit.content().readFully();
                    NetworkFrame f=new NetworkFrame(b);
                    if("listenNetwork".equals(f.command())) {
                        childMonitorController.singleAll(true);
                    }
                    circuit.content().writeBytes(b);//再写回去供后面打印
                }
                StringBuffer sb = new StringBuffer();
                circuit.print(sb);
                System.out.println(frame + "\r\n" + sb);
                System.out.print(">");
            }
        }, new IOnopen() {
            @Override
            public void onopen(NetworkFrame frame, INetworkPeer networkPeer) {
                System.out.println(String.format("The Network %s was Opened.", networkPeer.getNetworkName()));
            }
        }, new IOnmessage() {
            @Override
            public void onmessage(NetworkFrame frame, IServiceProvider site) {
                byte[] b = frame.content().readFully();
                NetworkCircuit circuit = new NetworkCircuit(b);
                if ("auth".equals(frame.command()) && circuit.status().equals("200")) {
                    //登录成功后开始工作
                    System.out.println(String.format("The Peer %s is Logged in to %s://%s:%s，Let`s get to work!", peer.peerName(), peer.getNodeProtocol(), peer.getNodeHost(), peer.getNodePort()));
                    System.out.println("——————————————使用说明——————————————");
                    System.out.println("       如不记得命令，可用man命令查询");
                    System.out.println("__________________________________");
                    System.out.println();
                    try {
                        lock.lock();
                        finished.signalAll();
                    } finally {
                        lock.unlock();
                    }
                    return;
                }

                StringBuffer sb = new StringBuffer();
                circuit.print(sb);
                System.out.println(frame + "\r\n" + sb);
//                if("infoNetwork".equals(frame.command())){//在子窗口中发向主网络回来的指令，其窗口前缀要在子窗口中
//                    System.out.print(String.format("%s>",circuit.head("Network-Name")));
//                }else {
//                    System.out.print(">");
//                }
            }
        }, new IOnclose() {
            @Override
            public void onclose(INetworkPeer networkPeer) {
                System.out.println(String.format("The Netowrk %s was Closed.", networkPeer.getNetworkName()));
                System.exit(0);
            }
        });//
        try {
            lock.lock();
            finished.await();
            IMonitor console = new PeerMonitor(childMonitorController);
            console.moniter(peer.site());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        } finally {
            lock.unlock();
        }

    }

    private static File getHomeDir(String fileName, CommandLine line) throws IOException {
        String usr = System.getProperty("user.dir");
        File f = new File(usr);
        File[] arr = f.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                if (name.startsWith(fileName)) {
                    return true;
                }
                return false;
            }
        });
        if (arr.length < 1 && !line.hasOption("debug")) {
            throw new IOException(fileName + " 程序集不存在.");
        }
        if (line.hasOption("debug")) {
            File[] da = new File(line.getOptionValue("debug")).listFiles(new FilenameFilter() {

                @Override
                public boolean accept(File dir, String name) {
                    if (name.startsWith(fileName)) {
                        return true;
                    }
                    return false;
                }
            });
            if (da.length < 0)
                throw new IOException("调试时不存在指定的必要jar包" + fileName);
            f = da[0];
        } else {
            f = arr[0];
        }
        return f;
    }




}
