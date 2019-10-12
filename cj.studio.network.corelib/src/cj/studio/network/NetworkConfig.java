package cj.studio.network;

import io.netty.channel.Channel;

import java.util.Map;

public class NetworkConfig {
    String name;
    String castmode;//multicast,unicast,feedbackcast

    public NetworkConfig() {
    }

    public NetworkConfig(String name, String castmode) {
        this.name = name;
        this.castmode = castmode;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCastmode() {
        return castmode;
    }

    public void setCastmode(String castmode) {
        this.castmode = castmode;
    }

}
