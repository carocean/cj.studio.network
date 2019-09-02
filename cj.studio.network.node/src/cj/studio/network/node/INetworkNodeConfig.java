package cj.studio.network.node;

import cj.studio.network.NetworkInfo;

import java.io.FileNotFoundException;
import java.util.Map;

public interface INetworkNodeConfig {
    void load(String home) throws FileNotFoundException;

    ServerInfo getServerInfo();

    Map<String, NetworkInfo> getGeneralNetworks();

    NetworkInfo getManagerNetwork();

    ReactorInfo getReactorInfo();
}
