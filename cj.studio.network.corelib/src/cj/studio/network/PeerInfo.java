package cj.studio.network;

import java.util.ArrayList;
import java.util.List;

public class PeerInfo {
    String peer;
    String user;
    String onlineTime;
    List<String> roles;

    public PeerInfo() {
        this.roles = new ArrayList<>();
    }

    public String getOnlineTime() {
        return onlineTime;
    }

    public void setOnlineTime(String onlineTime) {
        this.onlineTime = onlineTime;
    }

    public String getPeer() {
        return peer;
    }

    public void setPeer(String peer) {
        this.peer = peer;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setRoles(List<String> roles) {
        this.roles.addAll(roles);
    }
}
