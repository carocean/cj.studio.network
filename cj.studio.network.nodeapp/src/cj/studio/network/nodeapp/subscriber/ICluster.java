package cj.studio.network.nodeapp.subscriber;

import cj.studio.network.peer.IPeer;
import cj.studio.util.reactor.RemoteServiceNode;

public interface ICluster {
    boolean available();

    Object[] getNode(String key);

    void addNode(IPeer peer, SubscriberInfo info);

    void init(ISubscriberConfig subscriberConfig);


    void invalidNode(String nodeName);

    void validNode(String nodeName);

}
