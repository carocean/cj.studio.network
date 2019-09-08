package cj.studio.network.nodeapp;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.annotation.CjService;
import cj.studio.network.*;
import cj.studio.network.nodeapp.cluster.ClusterValve;
import cj.studio.network.nodeapp.strategy.PasswordAuthenticateStrategy;
import cj.studio.network.nodeapp.strategy.SystemAccessControllerStrategy;
import cj.studio.util.reactor.IPipeline;
import cj.studio.util.reactor.IValve;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

@CjService(name = "$.cj.studio.node.app", isExoteric = true)
public class NodeApplication implements INodeApplication {
    IRBACConfig rbacconfig;
    String masterNetworkName;
    List<IValve> valves;
    @Override
    public void onstart(String home,String masterNetworkName, IServiceProvider site) {
        this.masterNetworkName=masterNetworkName;
        this.valves=new ArrayList<>();
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
        for(IValve valve:valves){
            pipeline.remove(valve);
        }
    }

    @Override
    public void onactivedNetwork(UserPrincipal userPrincipal, INetwork network, IPipeline pipeline) {
        if(masterNetworkName.equals(network.getInfo().getName())){
            return;
        }
        IValve cluster=new ClusterValve();
        this.valves.add(cluster);
        pipeline.append(cluster);
    }
}
