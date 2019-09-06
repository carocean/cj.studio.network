package cj.studio.network.console.nw;

import cj.studio.network.console.BaseMonitor;
import cj.studio.network.console.Command;
import cj.studio.network.console.IMonitor;
import cj.studio.network.console.nw.cmd.SendNetworkCommand;
import cj.studio.network.console.nw.cmd.ByeNetworkCommand;
import cj.studio.network.console.nw.cmd.ViewNetworkCommand;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetworkMonitor extends BaseMonitor implements IMonitor {
    String networkName;
    AtomicBoolean bye;
    Scanner scanner;
    public NetworkMonitor(Scanner scanner,AtomicBoolean bye,String networkName) {
        this.networkName=networkName;
        this.bye=bye;
        this.scanner=scanner;
    }

    @Override
    protected String getPrefix() {
        return networkName+">";
    }

    @Override
    protected Map<String, Command> getCommands() {
        Map<String, Command> cmds = new HashMap<>();
        Command send = new SendNetworkCommand();
        cmds.put(send.cmd(), send);
        Command bye = new ByeNetworkCommand();
        cmds.put(bye.cmd(), bye);
        Command ls = new ViewNetworkCommand();
        cmds.put(ls.cmd(), ls);
        return cmds;
    }

    @Override
    protected Scanner getScanner() {
        return scanner;
    }

    @Override
    protected boolean isExit(String text) {
        return bye.get();
    }
}
