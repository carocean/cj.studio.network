package cj.studio.network.node;


import cj.studio.network.Frame;
import cj.ultimate.IDisposable;
import io.netty.channel.Channel;

public interface INetwork extends IDisposable {
    NetworkInfo getInfo();

    void addChannel(Channel ch);

    void cast(Channel source, Frame frame);

    void removeChannel(Channel channel);
}
