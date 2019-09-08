package cj.studio.network.nodeapp;

import cj.studio.network.nodeapp.strategy.UserInfo;

import java.io.FileNotFoundException;

public interface IRBACConfig {
    void load(String home) throws FileNotFoundException;

    Acl getAcl();

    boolean isEnableRBAC();

    UserInfo getUserInfo(String authUser);

    String getMasterNetworkName();
}
