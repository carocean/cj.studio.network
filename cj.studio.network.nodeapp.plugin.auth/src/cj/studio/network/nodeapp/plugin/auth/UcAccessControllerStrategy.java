package cj.studio.network.nodeapp.plugin.auth;

import cj.studio.ecm.IServiceProvider;
import cj.studio.network.*;

/**
 * 与用户中心对接
 */
public class UcAccessControllerStrategy implements IAccessControllerStrategy {
    public UcAccessControllerStrategy(IServiceProvider site) {

    }

    @Override
    public void access(NetworkFrame frame, UserPrincipal userPrincipal, INetwork network) throws AccessException {
        return;
    }
}
