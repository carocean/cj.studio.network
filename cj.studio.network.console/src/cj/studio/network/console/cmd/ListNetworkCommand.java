package cj.studio.network.console.cmd;

import cj.studio.network.console.CmdLine;
import cj.studio.network.console.Command;
import cj.studio.network.peer.IMasterNetworkPeer;
import cj.studio.network.peer.INetworkPeerContainer;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.IOException;

public class ListNetworkCommand extends Command {
    @Override
    public String cmd() {
        return "ls";
    }

    @Override
    public String cmdDesc() {
        return "列出网络。用法：ls";
    }

    @Override
    public Options options() {
        Options options = new Options();
        Option v = new Option("v", "view", true, "仅查看指定网络信息信息");
        options.addOption(v);
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
        INetworkPeerContainer container = (INetworkPeerContainer) cl.site().getService("$.peer.container");
        IMasterNetworkPeer master =(IMasterNetworkPeer) container.getMasterNetwork();
        if(cl.line().hasOption("v")){
            master.infoNetwork(cl.line().getOptionValue("v"));
        }else{
            master.listNetwork();
        }
        return false;
    }
}
