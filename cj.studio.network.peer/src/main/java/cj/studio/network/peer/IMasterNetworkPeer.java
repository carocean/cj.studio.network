package cj.studio.network.peer;

public interface IMasterNetworkPeer extends INetworkPeer {
    void createNetwork(String name, String castmode);

    void exitsNetwork(String name);

    void castmodeNetwork(String name, String castmode);

    void renameNetwork(String name, String newName);

    void removeNetwork(String name);

    void listNetwork();

    void infoNetwork(String name);
}
