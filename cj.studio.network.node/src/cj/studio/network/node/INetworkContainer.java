package cj.studio.network.node;

import java.util.Map;

public interface INetworkContainer {
    void refresh();

    boolean existsNetwork(String network);

    NetworkInfo getManagerNetworkInfo();
}
