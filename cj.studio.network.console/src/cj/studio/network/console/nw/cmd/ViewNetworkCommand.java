package cj.studio.network.console.nw.cmd;

import cj.studio.network.NetworkFrame;
import cj.studio.network.console.CmdLine;
import cj.studio.network.console.Command;
import cj.studio.network.peer.INetworkPeer;
import cj.studio.network.peer.INetworkPeerContainer;
import org.apache.commons.cli.Options;

import java.io.IOException;

public class ViewNetworkCommand extends Command {
    @Override
    public String cmd() {
        return "ls";
    }

    @Override
    public String cmdDesc() {
        return "查看当前网络。用法：ls";
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
        INetworkPeer networkPeer=(INetworkPeer) cl.site().getService("$.current");
        networkPeer.info();
        return false;
    }
}
