package cj.studio.network;

/**
 * 认证策略
 */
public interface IAuthenticateStrategy {
    UserPrincipal authenticate(String authUser, String authToken)throws AuthenticationException;

}
