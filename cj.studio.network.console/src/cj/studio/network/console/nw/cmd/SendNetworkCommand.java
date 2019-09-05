package cj.studio.network.console.nw.cmd;

import cj.studio.network.NetworkFrame;
import cj.studio.network.console.CmdLine;
import cj.studio.network.console.Command;
import cj.studio.network.console.ConsoleEditor;
import cj.studio.network.peer.INetworkPeer;
import cj.studio.network.peer.INetworkPeerContainer;
import cj.studio.network.peer.IPeer;
import cj.ultimate.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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
        return "发送侦。用法：send -t 100";
    }

    @Override
    public Options options() {
        Options options = new Options();
        Option f = new Option("t", "times", true, "[可省略]发送次数,默认是1次");
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
        INetworkPeer networkPeer = (INetworkPeer) cl.site().getService("$.current");
        StringBuffer sb = new StringBuffer();
        System.out.println(String.format("输入Frame，说明:"));
        System.out.println(String.format("\t- 第一行是请求头，如：put /path1/p2?xx=3&t=s my/1.0"));
        System.out.println(String.format("\t- 接着的行header，写法：key=value，然后回撤，其后接连多行均为header"));
        System.out.println(String.format("\t- 接着回撤一空行 为parameter，写法：key=value，然后回撤，其后接连多行均为parameter"));
        System.out.println(String.format("\t- 接着回撤两空行 为content，内容为任意输入"));
        System.out.println(String.format("\t- 如果无header且有参数则在请求行后回撤一个空行，如果有头有内容无参数，则在内容前回撤三个空行"));
        System.out.println(String.format("\t- 以!q号结输输入:"));
        //
        ConsoleEditor.readConsole("\t", "\r\n", ConsoleEditor.newReader(), sb);
        String text = sb.toString().trim();
        if (StringUtil.isEmpty(text)) {
            return true;
        }
        while (text.startsWith("\r\n")) {
            text = text.substring(2, text.length());
        }
        int pos = text.indexOf("\r\n");
        String frameline = "";
        String frameText = "";
        if (pos < 0) {
            frameline = text;
        } else {
            frameline = text.substring(0, pos);
            frameText = text.substring(pos + 2, text.length());
        }

        ByteBuf bb = Unpooled.buffer();
        NetworkFrame frame = new NetworkFrame(frameline, bb);
        if (!StringUtil.isEmpty(frameText)) {
            byte[] raw = frameText.getBytes();
            NetworkFrame frame2 = new NetworkFrame(raw);
            frame.add(frame2);
            frame2.dispose();
        }
        long times=1;
        if(line.hasOption("t")){
            times=Long.valueOf(line.getOptionValue("t"));
        }
        if(times<1){
            times=1;
        }
        for(int i=0;i<times;i++) {
            networkPeer.send(frame.copy());
        }
        return false;
    }
}
