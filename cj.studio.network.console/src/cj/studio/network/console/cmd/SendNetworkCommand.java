package cj.studio.network.console.cmd;

import cj.studio.network.NetworkFrame;
import cj.studio.network.console.CmdLine;
import cj.studio.network.console.Command;
import cj.studio.network.console.ConsoleEditor;
import cj.studio.network.peer.INetworkPeer;
import cj.studio.network.peer.INetworkPeerContainer;
import cj.studio.network.peer.IPeer;
import cj.ultimate.util.StringUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.*;
import java.util.List;

public class SendNetworkCommand extends Command {
    @Override
    public String cmd() {
        return "send";
    }

    @Override
    public String cmdDesc() {
        return "发送侦。用法：send networkName 如果向主网络发送可以省略网络名";
    }

    @Override
    public Options options() {
        Options options = new Options();
        Option f = new Option("t", "times", true, "[可省略]发送次数");
        options.addOption(f);
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
        INetworkPeer networkPeer=(INetworkPeer) cl.site().getService("$.network");
        PipedReader read=new PipedReader();
        BufferedReader reader=new BufferedReader(read);
        if(!ConsoleEditor.confirmConsole("!w","!q","\t",reader)){
            return true;
        }
        System.out.println(read);
//
//        NetworkFrame frame = new NetworkFrame("listNetwork / network/1.0");
//        networkPeer.send(frame);
        return false;
    }
}
