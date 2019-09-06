package cj.studio.network.console;

import cj.studio.network.console.cmd.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class PeerMonitor extends BaseMonitor {

    ChildMonitorController childMonitorController;
    public PeerMonitor(ChildMonitorController childMonitorController) {
        this.childMonitorController=childMonitorController;
    }

    @Override
    protected boolean isExit(String text) {
        return "bye".equals(text) || "exit".equals(text);
    }

    @Override
    protected Scanner getScanner() {
        return new Scanner(System.in);
    }

    @Override
    protected String getPrefix() {
        return ">";
    }

    @Override
    protected Map<String, Command> getCommands() {
        Map<String, Command> cmds = new HashMap<>();
        Command ls = new ListNetworkCommand();
        cmds.put(ls.cmd(), ls);
        Command createNetworkCommand = new CreateNetworkCommand();
        cmds.put(createNetworkCommand.cmd(), createNetworkCommand);
        Command remove = new RemoveNetworkCommand();
        cmds.put(remove.cmd(), remove);
        Command exists = new ExistsNetworkCommand();
        cmds.put(exists.cmd(), exists);
        Command rename = new RenameNetworkCommand();
        cmds.put(rename.cmd(), rename);
        Command castmode = new CastmodeNetworkCommand();
        cmds.put(castmode.cmd(), castmode);
        Command listen = new ListenNetworkCommand(this.childMonitorController);
        cmds.put(listen.cmd(), listen);
        return cmds;
    }
}
