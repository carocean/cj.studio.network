package cj.studio.network.console;

import cj.studio.ecm.IServiceProvider;
import cj.studio.network.peer.IPeer;
import cj.ultimate.util.StringUtil;

import java.util.Scanner;

public class PeerMonitor implements IPeerMonitor {
    @Override
    public void moniter(IServiceProvider site) {
        Scanner sc = new Scanner(System.in);
        IPeer peer=(IPeer) site.getService("$.peer");
        String prefix=">";
        System.out.print(prefix);
        while (sc.hasNextLine()) {
            String text = sc.nextLine();
            if("bye".equals(text)||"exit".equals(text)){
                break;
            }
            if(StringUtil.isEmpty(text)){
                System.out.print(prefix);
                continue;
            }
            System.out.println(text);
            System.out.print(prefix);
        }
    }
}
