package cj.studio.network.node;


import cj.studio.network.NetworkFrame;
import cj.ultimate.IDisposable;
import io.netty.channel.Channel;

public interface INetwork extends IDisposable {
    NetworkInfo getInfo();

    /**
     * 枚举网络内的peer名
     * @return
     */
    String[] enumPeerName();

    void addChannel(Channel ch);

    void cast(Channel source, NetworkFrame frame);

    void removeChannel(Channel channel);

    boolean existsChannel(Channel channel);

}
