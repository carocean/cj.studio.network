package cj.studio.network.node.combination;

import cj.studio.ecm.net.CircuitException;
import cj.studio.network.NetworkFrame;
import cj.studio.util.reactor.Event;
import cj.studio.util.reactor.IPipeline;
import cj.studio.util.reactor.IValve;
import io.netty.channel.Channel;

public class ListenValve implements IValve {
    @Override
    public void flow(Event e, IPipeline pipeline) throws CircuitException {
        NetworkFrame frame = (NetworkFrame) e.getParameters().get("frame");
        Channel channel = (Channel) e.getParameters().get("channel");
        if("NETWORK/1.0".equals(frame.protocol())&&"listenNetwork".equals(frame.command())){
            return;//peer端发来的网络侦听指令，拦截该指令然后丢弃。目的是客户端的侦听会自动触发reactor从而使得客户端加入网络
        }
        pipeline.nextFlow(e,this);
    }

    @Override
    public void nextError(Event e, Throwable error, IPipeline pipeline) throws CircuitException {
        NetworkFrame frame = (NetworkFrame) e.getParameters().get("frame");
        Channel channel = (Channel) e.getParameters().get("channel");
        if("NETWORK/1.0".equals(frame.protocol())&&"listenNetwork".equals(frame.command())){
            return;
        }
        pipeline.nextError(e,error,this);
    }
}
