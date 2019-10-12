package cj.studio.network.node;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.network.NetworkConfig;
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
    Map<String, NetworkConfig> networks;
    private String masterNetwork;
    private boolean isAutoCreate;
    private String home;


    @Override
    public void load(String home) throws FileNotFoundException {
        this.home = home;
        Yaml nodeyaml = new Yaml();
        String confNodeFile = String.format("%s%sconf%snode.yaml", home, File.separator, File.separator);
        Reader reader = new FileReader(confNodeFile);
        Map<String, Object> node = nodeyaml.load(reader);
        parseServerInfo(node);
        parseReactorInfo(node);
        parseNetworks(node);
    }

    @Override
    public String home() {
        return home;
    }

    @Override
    public String getMasterNetwork() {
        return masterNetwork;
    }


    @Override
    public boolean isAutoCreate() {
        return isAutoCreate;
    }

    private void parseNetworks(Map<String, Object> node) {
        networks = new HashMap<>();
        Map<String, Object> networksItem = (Map<String, Object>) node.get("networks");
        if (networksItem == null) {
            throw new EcmException("缺少networks配置");
        }
        Object auto = networksItem.get("isAutoCreate");
        this.isAutoCreate = auto == null ? false : (boolean) auto;

        masterNetwork = (String) networksItem.get("master");
        if (StringUtil.isEmpty(masterNetwork)) {
            masterNetwork = "master-network";
        }
        NetworkConfig managerInfo = new NetworkConfig();
        managerInfo.setCastmode("feedbackcast");
        managerInfo.setName(masterNetwork);
        this.networks.put(masterNetwork, managerInfo);
        CJSystem.logging().info(getClass(),String.format("配置的主网络名是：%s",masterNetwork));
        if (networksItem == null) return;
        List<Map<String, Object>> works = (List<Map<String, Object>>) networksItem.get("works");
        if (works == null) return;
        for (Map<String, Object> obj : works) {
            NetworkConfig info = new NetworkConfig((String) obj.get("name"), (String) obj.get("castmode"));
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
    public Map<String, NetworkConfig> getNetworks() {
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
