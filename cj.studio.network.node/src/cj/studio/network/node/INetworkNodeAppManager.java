package cj.studio.network.node;

import cj.studio.network.IAccessControllerStrategy;
import cj.studio.network.IAuthenticateStrategy;
import cj.studio.network.INetwork;
import cj.studio.network.UserPrincipal;
import cj.studio.util.reactor.IPipeline;

public interface INetworkNodeAppManager {
    void load(INetworkNodeConfig config);

    void onactivedNetwork(UserPrincipal userPrincipal, INetwork network, IPipeline pipeline);

    void oninactiveNetwork(INetwork network, IPipeline pipeline);

    IAuthenticateStrategy createAuthenticateStrategy(String authMode, INetwork network);

    IAccessControllerStrategy createAccessControllerStrategy();

    boolean isEnableRBAC();

}
