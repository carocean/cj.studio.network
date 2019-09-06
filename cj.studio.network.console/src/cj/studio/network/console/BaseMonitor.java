package cj.studio.network.console;

import cj.studio.ecm.IServiceProvider;
import cj.studio.network.peer.IPeer;
import cj.ultimate.util.StringUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public abstract class BaseMonitor implements IMonitor {

    protected abstract Map<String, Command> getCommands();
    protected abstract Scanner getScanner();
    @Override
    public void moniter(IServiceProvider site) throws ParseException, IOException {
        Scanner sc = getScanner();
        if(sc==null){
            sc=new Scanner(System.in);
        }
        IPeer peer = (IPeer) site.getService("$.peer");
        String prefix = getPrefix();
        if(StringUtil.isEmpty(prefix)){
            prefix=">";
        }
        System.out.print(prefix);
        Map<String, Command> commands = getCommands();
        while (sc.hasNextLine()) {
            String text = sc.nextLine();
            if (isExit(text)) {
                break;
            }
            if (StringUtil.isEmpty(text)) {
                System.out.print(prefix);
                continue;
            }
            String cmdName = parseCmd(text);
            if ("man".equals(cmdName)) {
                printMan(commands);
                System.out.print(prefix);
                continue;
            }
            Command cmd = commands.get(cmdName);
            if (cmd == null) {
                System.out.println(String.format("不认识的命令：%s", cmdName));
                System.out.print(prefix);
                continue;
            }
            String[] arr = text.split(" ");
            String args[] = new String[arr.length - 1];
            if (arr.length > 1) {
                System.arraycopy(arr, 1, args, 0, arr.length - 1);
            }
            GnuParser parser = new GnuParser();
            CommandLine the = parser.parse(cmd.options(), args);
            CmdLine cl = new CmdLine(cmdName, the, site);
            try {
                boolean isPrintPrefix = cmd.doCommand(cl);
                if (isPrintPrefix) {
                    System.out.print(prefix);
                }
            } catch (Throwable throwable) {
                System.out.println(String.format("错误：%s", throwable));
                System.out.print(prefix);
            }
        }
    }

    protected abstract boolean isExit(String text);


    protected abstract String getPrefix();


    private String parseCmd(String text) {
        while (text.startsWith(" ")) {
            text = text.substring(1, text.length());
        }
        int pos = text.indexOf(" ");
        if (pos < 0) {
            return text;
        }
        return text.substring(0, pos);
    }

    protected void printMan(
            Map<String, Command> cmds) {
        Set<String> set = cmds.keySet();
        for (String key : set) {
            Command cmd = cmds.get(key);
            HelpFormatter formatter = new HelpFormatter();
            if (cmd.options() != null)
                formatter.printHelp(600, cmd.cmd(), cmd.cmdDesc(), cmd.options(),
                        "----------------");
        }

    }
}
