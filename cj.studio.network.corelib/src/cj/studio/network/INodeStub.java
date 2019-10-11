package cj.studio.network;

public interface INodeStub {

    void send(String networkName,NetworkFrame frame);
    String peerName();

    String getNodeHost();

    String getNodeProtocol();

    int getNodePort();
}
