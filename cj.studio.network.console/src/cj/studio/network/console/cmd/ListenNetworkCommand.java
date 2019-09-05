package cj.studio.network.console.cmd;

import cj.studio.ecm.IServiceProvider;
import cj.studio.network.NetworkCircuit;
import cj.studio.network.NetworkFrame;
import cj.studio.network.console.CmdLine;
import cj.studio.network.console.Command;
import cj.studio.network.console.IMonitor;
import cj.studio.network.console.PeerMonitor;
import cj.studio.network.console.nw.NetworkMonitor;
import cj.studio.network.peer.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ListenNetworkCommand extends Command {
    INetworkPeer networkPeer;

    @Override
    public String cmd() {
        return "listen";
    }

    @Override
    public String cmdDesc() {
        return "侦听网络。用法：listen networkName";
    }

    @Override
    public Options options() {
        Options options = new Options();
        return options;
    }

    @Override
    public boolean doCommand(CmdLine cl) throws IOException {
        CommandLine line = cl.line();
        List<String> args = line.getArgList();
        if (args.isEmpty()) {
            System.out.println(String.format("错误：未指定网络名"));
            return true;
        }
        String name = args.get(0);
        ReentrantLock lock = new ReentrantLock();
        Condition finished = lock.newCondition();
        IPeer peer = (IPeer) cl.site().getService("$.peer");
        INetworkPeer networkPeer = peer.listen(name, new IOnerror() {
            @Override
            public void onerror(NetworkFrame frame, INetworkPeer networkPeer) {
                byte[] b = frame.content().readFully();
                NetworkCircuit circuit = new NetworkCircuit(b);
                StringBuffer sb = new StringBuffer();
                circuit.print(sb);
                System.out.println(frame + "\r\n" + sb);
                System.out.print(String.format("\t%s>", networkPeer.getNetworkName()));
            }
        }, new IOnopen() {
            @Override
            public void onopen(NetworkFrame frame, INetworkPeer networkPeer) {
                ListenNetworkCommand.this.networkPeer = networkPeer;
                System.out.println("已侦听网络：" + networkPeer.getNetworkName());
                try {
                    lock.lock();
                    finished.signalAll();
                } finally {
                    lock.unlock();
                }
                System.out.print(String.format("\t%s>", networkPeer.getNetworkName()));
            }
        }, new IOnmessage() {
            @Override
            public void onmessage(NetworkFrame frame, IServiceProvider site) {
                StringBuffer sb = new StringBuffer();
                frame.print(sb);
                System.out.println("---network-1---");
                System.out.println(sb);
                System.out.print(String.format("\t%s>", ((INetworkPeer) site.getService("$.current")).getNetworkName()));

            }
        }, new IOnclose() {
            @Override
            public void onclose(INetworkPeer networkPeer) {
                System.out.println("已断开网络：" + networkPeer.getNetworkName());
                System.out.print(String.format("\t%s>", networkPeer.getNetworkName()));
            }
        });
        try {
            lock.lock();
            finished.await();
            IMonitor console = new NetworkMonitor();
            console.moniter(networkPeer.site());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return false;
    }
}
