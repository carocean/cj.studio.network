package cj.studio.network.node.combination;

import cj.studio.ecm.net.CircuitException;
import cj.studio.network.NetworkCircuit;
import cj.studio.network.NetworkFrame;
import cj.studio.network.node.INetwork;
import cj.studio.network.node.INetworkContainer;
import cj.studio.util.reactor.Event;
import cj.studio.util.reactor.IPipeline;
import cj.studio.util.reactor.IValve;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

public class ListenValve implements IValve {
    INetworkContainer container;

    public ListenValve(INetworkContainer container) {
        this.container = container;
    }

    @Override
    public void flow(Event e, IPipeline pipeline) throws CircuitException {
        NetworkFrame frame = (NetworkFrame) e.getParameters().get("frame");
        Channel channel = (Channel) e.getParameters().get("channel");
        if ("NETWORK/1.0".equals(frame.protocol()) && "listenNetwork".equals(frame.command())) {//侦听指令就是将当前channel 加入network
            INetwork network = (INetwork) container.getNetwork(pipeline.key());
            if (!network.existsChannel(channel)) {
                network.addChannel(channel);
            }
            listenNetwork(e, pipeline);
            return;
        }
        pipeline.nextFlow(e, this);
    }

    @Override
    public void nextError(Event e, Throwable error, IPipeline pipeline) throws CircuitException {
        pipeline.nextError(e, error, this);
    }

    private void listenNetwork(Event e, IPipeline pipeline) throws CircuitException {
        NetworkFrame frame = (NetworkFrame) e.getParameters().get("frame");
        INetwork network = container.getNetwork(container.getMasterNetworkName());
        if (network == null) {
            return;
        }

        Channel channel = (Channel) e.getParameters().get("channel");
        if (!network.existsChannel(channel)) {
            network.addChannel(channel);
        }
        ByteBuf bb = Unpooled.buffer();
        NetworkFrame f = new NetworkFrame(frame.toString(), bb);

        NetworkCircuit c = new NetworkCircuit("network/1.0 200 ok");
        bb.writeBytes(c.toByteBuf());
        e.getParameters().put("frame", f);
        pipeline.nextFlow(e, this);
    }
}
