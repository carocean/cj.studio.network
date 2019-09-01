package cj.studio.network.node;

import cj.ultimate.util.StringUtil;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkNodeConfig implements INetworkNodeConfig {
    ServerInfo serverInfo;
    ReactorInfo reactorInfo;
    NetworkInfo managerNetwork;
    Map<String,NetworkInfo> generalNetworks;
    @Override
    public void load(String home) throws FileNotFoundException {
        Yaml nodeyaml = new Yaml();
        String confNodeFile=String.format("%s%sconf%snode.yaml",home, File.separator, File.separator);
        Reader reader= new FileReader(confNodeFile);
        Map<String,Object> node=nodeyaml.load(reader);
        parseServerInfo(node);
        parseReactorInfo(node);
        parseNetworks(node);
    }

    private void parseNetworks(Map<String, Object> node) {
        Map<String,Object> managerObj=(Map<String, Object>) node.get("managerNetwork");
        NetworkInfo managerInfo=null;
        if(managerObj==null){
            managerInfo=new NetworkInfo("manager.network","multicast");
        }else{
            managerInfo=new NetworkInfo((String)managerObj.get("name"),(String)managerObj.get("castmode"));
        }
        this.managerNetwork=managerInfo;
        generalNetworks=new HashMap<>();
        List<Map<String, Object>> generalNetworks=(List<Map<String, Object>>) node.get("generalNetworks");
        if(generalNetworks==null){
            return;
        }
        for(Map<String, Object> obj:generalNetworks){
            managerInfo=new NetworkInfo((String)obj.get("name"),(String)obj.get("castmode"));
        }
    }

    private void parseReactorInfo(Map<String, Object> node) {
        reactorInfo=ReactorInfo.parse(node);
    }

    private void parseServerInfo(Map<String, Object> node) {
        serverInfo=new ServerInfo();
        serverInfo.parse(node);
    }

    @Override
    public ReactorInfo getReactorInfo() {
        return reactorInfo;
    }

    @Override
    public ServerInfo getServerInfo() {
        return serverInfo;
    }
}
