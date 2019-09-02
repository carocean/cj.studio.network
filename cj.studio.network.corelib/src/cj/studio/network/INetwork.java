package cj.studio.network;


import cj.ultimate.IDisposable;
import io.netty.channel.Channel;

public interface INetwork extends IDisposable {
    NetworkInfo getInfo();

    void addChannel(Channel ch);

    void cast(Channel source,Frame frame);

    void removeChannel(Channel channel);
}
