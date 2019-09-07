package cj.studio.network.console.cmd;

import cj.studio.network.NetworkFrame;
import cj.studio.network.console.CmdLine;
import cj.studio.network.console.Command;
import cj.studio.network.peer.IMasterNetworkPeer;
import cj.studio.network.peer.INetworkPeer;
import cj.studio.network.peer.INetworkPeerContainer;
import cj.ultimate.util.StringUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.util.List;

public class RemoveNetworkCommand extends Command {
    @Override
    public String cmd() {
        return "remove";
    }

    @Override
    public String cmdDesc() {
        return "移除网络。用法：remove networkName";
    }

    @Override
    public Options options() {
        Options options = new Options();
//        Option m = new Option("m", "castmode", true, "[可省略]网络分发类型，有：unicast,multicast,feebackcast,默认为multicast");
//        options.addOption(m);
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
        @SuppressWarnings("unchecked")
        List<String> args = line.getArgList();
        if (args.isEmpty()) {
            System.out.println(String.format("错误：未指定网络名"));
            return true;
        }
        String name = args.get(0);

        INetworkPeerContainer container = (INetworkPeerContainer) cl.site().getService("$.peer.container");
        IMasterNetworkPeer master = (IMasterNetworkPeer)container.getMasterNetwork();
        master.removeNetwork(name);
        return false;
    }
}
