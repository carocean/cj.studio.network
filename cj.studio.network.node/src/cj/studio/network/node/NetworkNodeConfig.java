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
    Map<String, NetworkInfo> networks;
    private String masterNetwork;

    @Override
    public void load(String home) throws FileNotFoundException {
        Yaml nodeyaml = new Yaml();
        String confNodeFile = String.format("%s%sconf%snode.yaml", home, File.separator, File.separator);
        Reader reader = new FileReader(confNodeFile);
        Map<String, Object> node = nodeyaml.load(reader);
        parseServerInfo(node);
        parseReactorInfo(node);
        parseNetworks(node);

    }


    @Override
    public String getMasterNetwork() {
        return masterNetwork;
    }

    private void parseNetworks(Map<String, Object> node) {
        networks = new HashMap<>();
        Map<String, Object> networksItem = (Map<String, Object>) node.get("networks");
        if (networksItem != null) {
            masterNetwork = (String) networksItem.get("master");
        }
        if (StringUtil.isEmpty(masterNetwork)) {
            masterNetwork = "master-network";
        }
        NetworkInfo managerInfo = new NetworkInfo();
        managerInfo.setCastmode("feedbackcast");
        managerInfo.setName(masterNetwork);
        this.networks.put(masterNetwork, managerInfo);
        if (networksItem == null) return;
        List<Map<String, Object>> works = (List<Map<String, Object>>) networksItem.get("works");
        if (works == null) return;
        for (Map<String, Object> obj : works) {
            NetworkInfo info = new NetworkInfo((String) obj.get("name"), (String) obj.get("castmode"));
            this.networks.put(info.getName(), info);
        }
    }

    private void parseReactorInfo(Map<String, Object> node) {
        reactorInfo = ReactorInfo.parse(node);
    }

    private void parseServerInfo(Map<String, Object> node) {
        serverInfo = new ServerInfo();
        serverInfo.parse(node);
    }

    @Override
    public Map<String, NetworkInfo> getNetworks() {
        return networks;
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
