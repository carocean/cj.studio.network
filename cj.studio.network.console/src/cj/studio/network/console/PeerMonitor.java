package cj.studio.network.console;

import cj.studio.network.console.cmd.*;

import java.util.HashMap;
import java.util.Map;

public class PeerMonitor extends BaseMonitor {
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
        Command listen = new ListenNetworkCommand();
        cmds.put(listen.cmd(), listen);
        Command send = new SendNetworkCommand();
        cmds.put(send.cmd(), send);
        return cmds;
    }
}
