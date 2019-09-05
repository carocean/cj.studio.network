package cj.studio.network.node;

public class NetworkInfo {
    String name;
    String castmode;//multicast,unicast,feedbackcast

    public NetworkInfo() {
    }

    public NetworkInfo(String name, String castmode) {
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
