package cj.studio.network;

import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NetworkInfo {
    String name;
    String castmode;
    List<PeerInfo> peerInfos;


    public static NetworkInfo parse(NetworkConfig config, Map<String, Channel> channels) {
        NetworkInfo info = new NetworkInfo();
        info.name = config.name;
        info.castmode = config.castmode;
        info.peerInfos = new ArrayList<>();
        for (String peerName : channels.keySet()) {
            Channel ch = channels.get(peerName);
            if (ch == null) continue;
            AttributeKey<UserPrincipal> upKey = AttributeKey.valueOf("Peer-UserPrincipal");
            UserPrincipal userPrincipal = ch.attr(upKey).get();
            if (userPrincipal == null) continue;
            PeerInfo pi = new PeerInfo();
            pi.setPeer(peerName);
            pi.setUser(userPrincipal.getName());
            pi.setRoles(userPrincipal.roles);
            info.peerInfos.add(pi);
        }
        return info;
    }

    public String getName() {
        return name;
    }

    public String getCastmode() {
        return castmode;
    }

    public List<PeerInfo> getPeerInfos() {
        return peerInfos;
    }
}
