package cj.studio.network.peer;

import cj.studio.network.NetworkFrame;

import java.util.Map;

public interface IConnection {
    String getHost();

    String getProtocol();

    int getPort();

    void connect(String protocol, String ip, int port, Map<String, String> props);

    void close();

    void send(NetworkFrame frame);

    boolean isConnected();

}
