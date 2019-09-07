package cj.studio.network.console.cmd;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.net.CircuitException;
import cj.studio.network.NetworkCircuit;
import cj.studio.network.NetworkFrame;
import cj.studio.network.console.ChildMonitorController;
import cj.studio.network.console.CmdLine;
import cj.studio.network.console.Command;
import cj.studio.network.console.IMonitor;
import cj.studio.network.console.nw.NetworkMonitor;
import cj.studio.network.peer.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ListenNetworkCommand extends Command {
    private ChildMonitorController childMonitorController;
    INetworkPeer networkPeer;


    public ListenNetworkCommand(ChildMonitorController childMonitorController) {
        this.childMonitorController = childMonitorController;
    }

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
        @SuppressWarnings("unchecked")
        List<String> args = line.getArgList();
        if (args.isEmpty()) {
            System.out.println(String.format("错误：未指定网络名"));
            return true;
        }
        String name = args.get(0);
        IPeer peer = (IPeer) cl.site().getService("$.peer");
        AtomicBoolean bye = new AtomicBoolean();
        Scanner scanner = new Scanner(System.in);
        INetworkPeer networkPeer = peer.listen(name, new IOnerror() {
            @Override
            public void onerror(NetworkFrame frame, NetworkCircuit circuit, INetworkPeer networkPeer) {
                StringBuffer sb = new StringBuffer();
                frame.print(sb);
                System.out.println(frame + "\r\n" + sb);
            }
        }, new IOnopen() {
            @Override
            public void onopen(NetworkFrame frame, INetworkPeer networkPeer) {
                ListenNetworkCommand.this.networkPeer = networkPeer;
//                System.out.println("已侦听网络：" + networkPeer.getNetworkName());
                childMonitorController.singleAll(false);
            }
        }, new IOnmessage() {
            @Override
            public void onmessage(NetworkFrame frame, IServiceProvider site) {
                StringBuffer sb = new StringBuffer();
                frame.print(sb);
                System.out.println(sb);

            }
        }, new IOnclose() {

            @Override
            public void onclose(INetworkPeer networkPeer) {
                bye.getAndSet(true);
                scanner.reset();
//                System.out.println("已断开网络：" + networkPeer.getNetworkName());
            }
        });
        try {
            childMonitorController.await();
        } catch (InterruptedException e) {
            return false;
        }
        if (!childMonitorController.isNotEntryChildMonitor()) {
            IMonitor console = new NetworkMonitor(scanner, bye, networkPeer.getNetworkName());
            try {
                console.moniter(networkPeer.site());
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

}
