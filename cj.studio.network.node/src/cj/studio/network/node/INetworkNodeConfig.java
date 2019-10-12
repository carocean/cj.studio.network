package cj.studio.network.node;

import cj.studio.network.NetworkConfig;

import java.io.FileNotFoundException;
import java.util.Map;

public interface INetworkNodeConfig {
    void load(String home) throws FileNotFoundException;
    String home();
    ServerInfo getServerInfo();

    Map<String, NetworkConfig> getNetworks();


    String getMasterNetwork();

    ReactorInfo getReactorInfo();


    boolean isAutoCreate();


}
