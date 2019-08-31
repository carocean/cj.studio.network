package cj.studio.network.node;

import org.yaml.snakeyaml.Yaml;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public class NetworkNodeConfig implements INetworkNodeConfig {
    ServerInfo serverInfo;
    @Override
    public void load(String home) throws FileNotFoundException {
        Yaml nodeyaml = new Yaml();
        Reader reader= new FileReader(home);
        Map<String,Object> node=nodeyaml.load(reader);
        parseServerInfo(node);
    }

    private void parseServerInfo(Map<String, Object> node) {
        System.out.println(node);
    }
}
