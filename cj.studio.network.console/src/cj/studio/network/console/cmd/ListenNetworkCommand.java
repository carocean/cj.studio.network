package cj.studio.network.console.cmd;

import cj.studio.ecm.IServiceProvider;
import cj.studio.network.NetworkCircuit;
import cj.studio.network.NetworkFrame;
import cj.studio.network.console.CmdLine;
import cj.studio.network.console.Command;
import cj.studio.network.peer.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.util.List;

public class ListenNetworkCommand extends Command {
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
//        Option f = new Option("f", "forward", false, "仅列出forward连结点");
//        options.addOption(f);
//        Option b = new Option("b", "backward", false, "仅列出backward连结点");
//        options.addOption(b);
//        Option s = new Option("s", "socket", false, "仅列出sockets");
//        options.addOption(s);
//        Option u = new Option("t", "tt", false, "开启即时监控");
//        options.addOption(u);
        // Option p = new Option("p", "password",true, "密码");
        // options.addOption(p);
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
        IPeer peer = (IPeer) cl.site().getService("$.peer");
        INetworkPeer networkPeer = peer.listen(name, new IOnerror() {
            @Override
            public void onerror(NetworkFrame frame, INetworkPeer networkPeer) {
                byte[] b = frame.content().readFully();
                NetworkCircuit circuit = new NetworkCircuit(b);
                StringBuffer sb = new StringBuffer();
                circuit.print(sb);
                System.out.println(frame + "\r\n" + sb);
                System.out.print(">");
            }
        }, new IOnopen() {
            @Override
            public void onopen(NetworkFrame frame, INetworkPeer networkPeer) {
                System.out.println("已侦听网络：" + networkPeer.getNetworkName());
                System.out.print(">");
            }
        }, new IOnmessage() {
            @Override
            public void onmessage(NetworkFrame frame, IServiceProvider site) {
                StringBuffer sb = new StringBuffer();
                frame.print(sb);
                System.out.println("---network-1---");
                System.out.println(sb);
                System.out.print(">");

            }
        }, new IOnclose() {
            @Override
            public void onclose(INetworkPeer networkPeer) {
                System.out.println("已断开网络：" + networkPeer.getNetworkName());
                System.out.print(">");
            }
        });
        return false;
    }
}
