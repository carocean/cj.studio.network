package cj.studio.network.peer;

import cj.studio.network.Frame;

import java.util.Map;

public interface IConnection {
    void connect(String protocol, String ip, int port, Map<String, String> props);

    void close();

    void send(Frame frame);

    boolean isConnected();

}
