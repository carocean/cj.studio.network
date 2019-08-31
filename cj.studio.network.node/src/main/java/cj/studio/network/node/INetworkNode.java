package cj.studio.network.node;

import java.io.FileNotFoundException;

public interface INetworkNode {
    void entrypoint(String home) throws FileNotFoundException;
}
