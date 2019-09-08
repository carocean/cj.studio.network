package cj.studio.network.nodeapp;

public class RBACCResource {
    String network;//支持通配符*，关键字：$root是主网，!root是所有工作网
    String command;//支持通配符*
    public RBACCResource() {
    }

    public RBACCResource(String network, String command) {
        this.network = network;
        this.command = command;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }
}
