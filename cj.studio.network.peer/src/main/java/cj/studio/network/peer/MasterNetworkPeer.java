package cj.studio.network.peer;

import cj.studio.ecm.IServiceProvider;
import cj.studio.network.NetworkFrame;

public class MasterNetworkPeer extends NetworkPeer implements IMasterNetworkPeer {

    public MasterNetworkPeer(IConnection connection, String networkName, IOnerror onerror, IOnopen onopen, IOnmessage onmessage, IOnclose onclose, IServiceProvider site) {
        super(connection, networkName, onerror, onopen, onmessage, onclose, site);
    }

    @Override
    public void createNetwork(String name, String castmode) {
        NetworkFrame frame = new NetworkFrame("createNetwork / network/1.0");
        frame.head("Network-Name", name);
        frame.head("Network-Castmode", castmode);
        this.send(frame);
    }

    @Override
    public void exitsNetwork(String name) {
        NetworkFrame frame = new NetworkFrame("existsNetwork / network/1.0");
        frame.head("Network-Name", name);
        this.send(frame);
    }

    @Override
    public void castmodeNetwork(String name, String castmode) {
        NetworkFrame frame = new NetworkFrame("changeCastmode / network/1.0");
        frame.head("Network-Name", name);
        frame.head("Network-Castmode", castmode);
        this.send(frame);
    }

    @Override
    public void renameNetwork(String name, String newName) {
        NetworkFrame frame = new NetworkFrame("renameNetwork / network/1.0");
        frame.head("Network-Name", name);
        frame.head("New-Network-Name", newName);
        this.send(frame);
    }

    @Override
    public void removeNetwork(String name) {
        NetworkFrame frame = new NetworkFrame("removeNetwork / network/1.0");
        frame.head("Network-Name", name);
        this.send(frame);
    }

    @Override
    public void listNetwork() {
        NetworkFrame frame = new NetworkFrame("listNetwork / network/1.0");
        this.send(frame);
    }

    @Override
    public void infoNetwork(String name) {
        NetworkFrame frame=new NetworkFrame("infoNetwork / network/1.0");
        frame.head("Network-Name",name);
        this.send(frame);//查网络信息是主网络命令
    }


}
