package cj.studio.network.node.combination;

import cj.studio.ecm.net.CircuitException;
import cj.studio.network.node.INetwork;
import cj.studio.network.node.INetworkContainer;
import cj.studio.util.reactor.Event;
import cj.studio.util.reactor.IPipeline;
import cj.studio.util.reactor.IValve;
import io.netty.channel.Channel;

public class JoinChannelValve implements IValve {
    INetworkContainer container;
    public JoinChannelValve(INetworkContainer container) {
        this.container=container;
    }

    @Override
    public void flow(Event e, IPipeline pipeline) throws CircuitException {
        INetwork network = container.getManagerNetwork();
        Channel channel = (Channel) e.getParameters().get("channel");
        if (!network.existsChannel(channel)) {
            network.addChannel(channel);
        }
        pipeline.nextFlow(e,this);
    }

    @Override
    public void nextError(Event e, Throwable error, IPipeline pipeline) throws CircuitException {
        pipeline.nextError(e,error,this);
    }
}
