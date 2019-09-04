package cj.studio.network.node;

import cj.studio.network.NetworkFrame;

public interface INetworkNodeAppManager {


    String auth(String authMode, String authUser, String authToken);

    void load(String home);

}
