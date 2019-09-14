package cj.studio.network.nodeapp.subscriber;

import cj.studio.ecm.IServiceProvider;

public interface ISubscriberContainer {
    ISubscriberConfig getSubscriberConfig();

    void start(String home, IServiceProvider site);

}
