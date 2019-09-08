package cj.studio.network.node.combination.valve;

import cj.studio.ecm.net.CircuitException;
import cj.studio.network.INetwork;
import cj.studio.network.node.INetworkContainer;
import cj.studio.util.reactor.Event;
import cj.studio.util.reactor.IPipeline;
import cj.studio.util.reactor.IValve;
import io.netty.channel.Channel;

public class CheckAutoCreateNetworkValve implements IValve {
    INetworkContainer container;

    public CheckAutoCreateNetworkValve(INetworkContainer container) {
        this.container = container;
    }

    @Override
    public void flow(Event e, IPipeline pipeline) throws CircuitException {
        Channel channel = (Channel) e.getParameters().get("channel");
        if (container.isAutoCreateNetwork()) {
            INetwork network = (INetwork) container.getNetwork(pipeline.key());
            if (!network.existsChannel(channel)) {
                network.addChannel(channel);
            }
        }
        pipeline.nextFlow(e, this);
    }

    @Override
    public void nextError(Event e, Throwable error, IPipeline pipeline) throws CircuitException {
        if (container.isAutoCreateNetwork()) {

        }
        pipeline.nextError(e, error, this);
    }

}
