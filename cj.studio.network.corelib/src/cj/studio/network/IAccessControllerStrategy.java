package cj.studio.network;



public interface IAccessControllerStrategy {
    void access(NetworkFrame frame,UserPrincipal userPrincipal, INetwork network)throws AccessException;

}
