package cj.studio.network.peer;

import cj.studio.ecm.IServiceProvider;
import cj.studio.network.NetworkFrame;

import java.util.Map;

public interface IConnection extends IServiceProvider {
    String getHost();

    String getProtocol();

    int getPort();

    void connect(String protocol, String ip, int port, Map<String, String> props);

    void close();

    void send(NetworkFrame frame);

    boolean isConnected();
    void reconnect();

}
