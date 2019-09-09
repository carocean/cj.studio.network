package cj.studio.network.nodeapp.subscriber;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ISubscriberConfig {
    void load(String home) throws FileNotFoundException;

    Collection<SubscriberInfo> getSubscribers();
}
