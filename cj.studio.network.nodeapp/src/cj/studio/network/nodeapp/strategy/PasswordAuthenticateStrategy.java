package cj.studio.network.nodeapp.strategy;

import cj.studio.network.AuthenticationException;
import cj.studio.network.IAuthenticateStrategy;
import cj.studio.network.UserPrincipal;
import cj.studio.network.nodeapp.IRBACConfig;

import java.util.Map;

public class PasswordAuthenticateStrategy implements IAuthenticateStrategy {
    IRBACConfig config;

    public PasswordAuthenticateStrategy(IRBACConfig config) {
        this.config = config;
    }

    @Override
    public synchronized UserPrincipal authenticate(String authUser, String authToken) throws AuthenticationException {
        UserInfo info = config.getUserInfo(authUser);
        if (info == null) {
            throw new AuthenticationException(String.format("用户不存在：%s",authUser));
        }
        if (!info.getPwd().equals(authToken)) {
            throw new AuthenticationException(String.format("用户：%s 密码错误",authUser));
        }
        UserPrincipal userPrincipal = new UserPrincipal(info.getName());
        for (String role : info.getRoles()) {
            userPrincipal.addRole(role);
        }
        return userPrincipal;
    }
}
