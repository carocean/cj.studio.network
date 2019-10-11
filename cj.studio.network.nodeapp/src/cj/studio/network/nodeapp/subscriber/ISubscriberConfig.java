package cj.studio.network.nodeapp.subscriber;

import java.io.FileNotFoundException;
import java.util.Collection;

public interface ISubscriberConfig {
    void load(String home) throws FileNotFoundException;

    String getBalance();

    Collection<SubscriberInfo> getSubscribers();

    String home();

    int getVNodeCount();
}
