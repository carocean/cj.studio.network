package cj.studio.network.node;

import cj.studio.network.NetworkInfo;
import cj.studio.network.UserPrincipal;

import java.io.FileNotFoundException;
import java.util.Map;

public interface INetworkNodeConfig {
    void load(String home) throws FileNotFoundException;
    String home();
    ServerInfo getServerInfo();

    Map<String, NetworkInfo> getNetworks();


    String getMasterNetwork();

    ReactorInfo getReactorInfo();


    boolean isAutoCreate();


}
