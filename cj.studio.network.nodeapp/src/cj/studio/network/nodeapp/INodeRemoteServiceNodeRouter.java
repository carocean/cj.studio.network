package cj.studio.network.nodeapp;

import cj.studio.util.reactor.IRemoteServiceNodeRouter;
import cj.studio.util.reactor.RemoteServiceNode;

public interface INodeRemoteServiceNodeRouter extends IRemoteServiceNodeRouter {
    void invalidNode(RemoteServiceNode remoteServiceNode);

    void validNode(RemoteServiceNode remoteServiceNode);

    void available(boolean b);

    boolean available();

}
