package cj.studio.network.nodeapp.subscriber;

import cj.studio.ecm.EcmException;
import cj.ultimate.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SubscribeNetwork {
    String network;
    List<String> castToLocals;

    public void setNetwork(String network) {
        this.network = network;
    }


    public List<String> getCastToLocals() {
        return castToLocals;
    }

    public String getNetwork() {
        return network;
    }

    public void parse(Map<String, Object> one) {
        this.network = one.get("network") == null ? "" : one.get("network") + "";
        if (StringUtil.isEmpty(network)) {
            throw new EcmException("network未指定");
        }
        castToLocals = (List<String>) one.get("castToLocals");
        if (castToLocals == null) {
            castToLocals = new ArrayList<>();
        }

    }
}
