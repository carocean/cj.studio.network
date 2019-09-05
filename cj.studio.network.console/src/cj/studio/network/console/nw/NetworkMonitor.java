package cj.studio.network.console.nw;

import cj.studio.network.console.BaseMonitor;
import cj.studio.network.console.Command;
import cj.studio.network.console.IMonitor;
import cj.studio.network.console.nw.cmd.SendNetworkCommand;

import java.util.HashMap;
import java.util.Map;

public class NetworkMonitor extends BaseMonitor implements IMonitor {
    @Override
    protected Map<String, Command> getCommands() {
        Map<String, Command> cmds = new HashMap<>();
        Command send = new SendNetworkCommand();
        cmds.put(send.cmd(), send);
        return cmds;
    }
}
