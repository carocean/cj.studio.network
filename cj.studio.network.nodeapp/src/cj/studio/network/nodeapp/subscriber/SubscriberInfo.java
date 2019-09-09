package cj.studio.network.nodeapp.subscriber;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.ultimate.util.StringUtil;

import java.util.*;

public class SubscriberInfo {
    String peerName;
    String masterNetworkName;
    private String token;
    private String nodeAddress;
    private String authMode;
    private String user;
    Map<String,SubscribeNetwork> subscribeNetworks;
    private boolean enable;

    public SubscriberInfo() {
        subscribeNetworks = new HashMap<>();
    }

    public void parse(Map<String, Object> info) {
        this.peerName = info.get("peername") == null ? "" : info.get("peername")+"";
        this.masterNetworkName = info.get("masterNetworkName") == null ? "" : info.get("masterNetworkName")+"";
        if (StringUtil.isEmpty(this.masterNetworkName)) {
            throw new EcmException(String.format("主网络未指定"));
        }
        this.user = info.get("user") == null ? "" : info.get("user")+"";
        this.authMode = info.get("authmode") == null ? "" : info.get("authmode")+"";
        this.token = info.get("token") == null ? "" : info.get("token")+"";
        this.nodeAddress = info.get("nodeAddress") == null ? "" :  info.get("nodeAddress")+"";
        this.enable=info.get("enable") == null ? true :(boolean) info.get("enable");
        Object subscribes = info.get("subscribeNetworks");
        if (subscribes != null) {
            parseSubscribes((List<Map<String, Object>>) subscribes);
        }
    }

    private void parseSubscribes(List<Map<String, Object>> subscribes) {
        for(Map<String, Object> one:subscribes){
            SubscribeNetwork sn=new SubscribeNetwork();
            sn.parse(one);
            if(this.subscribeNetworks.containsKey(sn.getNetwork())){
                CJSystem.logging().warn(getClass(),"配置冲突，多处订阅同一网络");
            }
            this.subscribeNetworks.put(sn.getNetwork(),sn);
        }
    }

    public boolean isEnable() {
        return enable;
    }

    public Collection<SubscribeNetwork> getSubscribeNetworks() {
        return subscribeNetworks.values();
    }

    public String getPeerName() {
        return peerName;
    }

    public String getMasterNetworkName() {
        return masterNetworkName;
    }

    public String getNodeAddress() {
        return nodeAddress;
    }

    public String getAuthMode() {
        return authMode;
    }

    public String getUser() {
        return user;
    }

    public String getToken() {
        return token;
    }

    public boolean containsSubscriberNetwork(String networkName) {
        return this.subscribeNetworks.containsKey(networkName);
    }

    public SubscribeNetwork getSubscriberNetwork(String networkName) {
        return this.subscribeNetworks.get(networkName);
    }
}
