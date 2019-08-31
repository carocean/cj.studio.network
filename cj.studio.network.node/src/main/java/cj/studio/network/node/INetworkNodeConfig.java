package cj.studio.network.node;

import java.io.FileNotFoundException;

public interface INetworkNodeConfig {
    void load(String home) throws FileNotFoundException;
}
