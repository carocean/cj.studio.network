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

    /**
     * 禁止重连。原因或许是peer认证失败了
     */
    void forbiddenReconnect();

    boolean isForbiddenReconnect();

}
