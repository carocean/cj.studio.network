package cj.studio.network;


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

    void removeChannel(Channel channel);


    void cast(Channel source, NetworkFrame frame);


    boolean existsChannel(Channel channel);

    INetwork createReference();

    void rename(String newNetworkName);

    void changeCastmode(String castmode);

}
