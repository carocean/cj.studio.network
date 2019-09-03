package cj.studio.network.node.combination;

import cj.studio.ecm.net.CircuitException;
import cj.studio.network.Circuit;
import cj.studio.network.Frame;
import cj.studio.network.node.INetwork;
import cj.studio.network.node.INetworkContainer;
import cj.studio.util.reactor.Event;
import cj.studio.util.reactor.IPipeline;
import cj.studio.util.reactor.IValve;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;


/**
 * 分发侦到网络。最后一个valve
 */
public class CastValve implements IValve {
    @Override
    public void flow(Event e, IPipeline pipeline) throws CircuitException {
        INetwork network = (INetwork) pipeline.attachment();
        Frame frame = (Frame) e.getParameters().get("frame");
        Channel channel = (Channel) e.getParameters().get("channel");
        network.cast(channel, frame);
    }

    @Override
    public void nextError(Event e, Throwable error, IPipeline pipeline) throws CircuitException {
        //如果出错则发送给管理网络
        INetworkContainer container = (INetworkContainer) pipeline.site().getService("$.network.container");
        INetwork manager = container.getNetwork(container.getManagerNetworkInfo().getName());
        Frame frame = (Frame) e.getParameters().get("frame");
        Channel channel = (Channel) e.getParameters().get("channel");
        ByteBuf bb= Unpooled.buffer();
        Frame f = new Frame(String.format("error /%s network/1.0",pipeline.key()), bb);
        Circuit c=new Circuit("network/1.0 200 ok");
        String status = "";
        String message = "";
        CircuitException ce = CircuitException.search(error);
        if (ce != null) {
            status = ce.getStatus();
            message = ce.getMessage();
        } else {
            status = "500";
            message = error.getMessage();
        }
        c.head("message", message);
        c.head("status", status);
        c.content().writeBytes(frame.toBytes());
        manager.cast(channel, f);
    }
}
