package cj.studio.network.node;

import java.io.FileNotFoundException;
import java.util.Map;

public interface INetworkNodeConfig {
    void load(String home) throws FileNotFoundException;

    ServerInfo getServerInfo();

    Map<String, NetworkInfo> getNetworks();

    String getMasterNetwork();
    ReactorInfo getReactorInfo();
}
