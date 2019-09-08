package cj.studio.network.nodeapp.strategy;

import cj.studio.ecm.CJSystem;
import cj.studio.network.*;
import cj.studio.network.nodeapp.Ace;
import cj.studio.network.nodeapp.Acl;
import cj.studio.network.nodeapp.IRBACConfig;

public class SystemAccessControllerStrategy implements IAccessControllerStrategy {
    IRBACConfig rbacconfig;

    public SystemAccessControllerStrategy(IRBACConfig rbacconfig) {
        this.rbacconfig = rbacconfig;
    }

    @Override
    public void access(NetworkFrame frame, UserPrincipal userPrincipal, INetwork network) throws AccessException {
        Acl acl = rbacconfig.getAcl();
        for (int i = 0; i < acl.denyCount(); i++) {
            Ace ace = acl.deny(i);
            if (userPrincipal.hasRole(ace.getRole())) {
                if (ace.hasInExcept(network.getInfo().getName(), rbacconfig.getMasterNetworkName().equals(network.getInfo().getName()), frame.command())) {
                    return;//有权
                }
                throw new AccessException("无权限");
            }
        }
        for (int i = 0; i < acl.allowCount(); i++) {
            Ace ace = acl.allow(i);
            if (userPrincipal.hasRole(ace.getRole())) {
                if (ace.hasInExcept(network.getInfo().getName(), rbacconfig.getMasterNetworkName().equals(network.getInfo().getName()), frame.command())) {
                    throw new AccessException("无权限");
                }
                return;//一定是return结束方法，表示显式充许权限
            }
        }
        throw new AccessException("无权限");
    }
}
