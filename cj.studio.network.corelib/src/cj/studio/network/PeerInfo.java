package cj.studio.network;

import java.util.ArrayList;
import java.util.List;

public class PeerInfo {
    String peer;
    String user;
    List<String> roles;

    public PeerInfo() {
        this.roles = new ArrayList<>();
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
