package cj.studio.network.nodeapp.subscriber;

import cj.studio.ecm.CJSystem;
import cj.ultimate.util.StringUtil;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.*;

public class SubscriberConfig implements ISubscriberConfig {
    Map<String, SubscriberInfo> subscribers;
    private String balance;
    private String home;
    private int vNodeCount;


    @Override
    public String home() {
        return home;
    }

    @Override
    public void load(String home) throws FileNotFoundException {
        this.home = home;
        subscribers = new HashMap<>();
        Yaml nodeyaml = new Yaml();
        String confFile = String.format("%s%sconf%ssubscribers.yaml", home, File.separator, File.separator);
        Reader reader = new FileReader(confFile);
        Map<String, Object> cluster = nodeyaml.load(reader);

        Object vNodeCount = cluster.get("vNodeCount");
        String strvNodeCount = vNodeCount == null ? "10" : vNodeCount + "";
        this.vNodeCount = Integer.valueOf(strvNodeCount);
        Object balance = cluster.get("balance");
        String strbalance = balance == null ? "unorientor" : balance + "";
        if (StringUtil.isEmpty(strbalance)) {
            strbalance = "unorientor";
        }
        this.balance = strbalance;
        List<Map<String, Object>> list = (List<Map<String, Object>>) cluster.get("nodes");
        if (list != null) {
            for (Map<String, Object> one : list) {
                SubscriberInfo info = new SubscriberInfo();
                try {
                    info.parse(one);
                } catch (Exception e) {
                    CJSystem.logging().warn(getClass(), String.format("解析失败，已忽略该项：%s", one));
                    continue;
                }
                if (!info.isEnable()) {
                    continue;
                }
                if (subscribers.containsKey(info.peerName)) {
                    CJSystem.logging().warn(getClass(), String.format("已包含peer：%s，已忽略一个", info.peerName));
                    continue;
                }
                subscribers.put(info.peerName, info);
            }
        }
    }

    @Override
    public int getVNodeCount() {
        return vNodeCount;
    }

    @Override
    public String getBalance() {
        return balance;
    }

    @Override
    public Collection<SubscriberInfo> getSubscribers() {
        return subscribers.values();
    }
}
