package cj.studio.network.nodeapp.subscriber;

import cj.studio.ecm.CJSystem;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.*;

public class SubscriberConfig implements ISubscriberConfig {
    Map<String,SubscriberInfo> subscribers;
    @Override
    public void load(String home) throws FileNotFoundException {
        subscribers=new HashMap<>();
        Yaml nodeyaml = new Yaml();
        String confFile = String.format("%s%sconf%ssubscribers.yaml", home, File.separator, File.separator);
        Reader reader = new FileReader(confFile);
        List<Map<String,Object>> list = nodeyaml.load(reader);
        if(list!=null) {
            for (Map<String, Object> one : list) {
                SubscriberInfo info = new SubscriberInfo();
                try {
                    info.parse(one);
                }catch (Exception e){
                    CJSystem.logging().warn(getClass(), String.format("解析失败，已忽略该项：%s", one));
                    continue;
                }
                if(!info.isEnable()){
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
    public Collection<SubscriberInfo> getSubscribers() {
        return subscribers.values();
    }
}