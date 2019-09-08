package cj.studio.network.nodeapp;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.annotation.CjService;
import cj.studio.network.*;
import cj.studio.network.nodeapp.strategy.PasswordAuthenticateStrategy;
import cj.studio.network.nodeapp.strategy.SystemAccessControllerStrategy;
import cj.studio.util.reactor.IPipeline;

import java.io.FileNotFoundException;

@CjService(name = "$.cj.studio.node.app", isExoteric = true)
public class NodeApplication implements INodeApplication {
    IRBACConfig rbacconfig;
    @Override
    public void onstart(String home,String masterNetworkName, IServiceProvider site) {
        rbacconfig = new RBACConfig(masterNetworkName);
        try {
            rbacconfig.load(home);
        } catch (FileNotFoundException e) {
            throw new EcmException(e);
        }
    }

    @Override
    public boolean isEnableRBAC() {
        return rbacconfig.isEnableRBAC();
    }

    @Override
    public IAuthenticateStrategy createAuthenticateStrategy(String authMode, INetwork network) {
        switch (authMode) {
            case "auth.password":
                return new PasswordAuthenticateStrategy(rbacconfig);
//            case "auth.jwt":
//                return new JwtAuthenticateStrategy();
        }
        return null;
    }

    @Override
    public SystemAccessControllerStrategy createAccessControllerStrategy() {
        return new SystemAccessControllerStrategy(rbacconfig);
    }

    @Override
    public void oninactiveNetwork(INetwork network, IPipeline pipeline) {

    }

    @Override
    public void onactivedNetwork(UserPrincipal userPrincipal, INetwork network, IPipeline pipeline) {
    }
}
