package cj.studio.network.node;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
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
        String confNodeFile=String.format("%s%sconf%snode.yaml",home, File.separator, File.separator);
        Reader reader= new FileReader(confNodeFile);
        Map<String,Object> node=nodeyaml.load(reader);
        parseServerInfo(node);
    }

    private void parseServerInfo(Map<String, Object> node) {
        serverInfo=new ServerInfo();
        serverInfo.parse(node);
    }
    @Override
    public ServerInfo getServerInfo() {
        return serverInfo;
    }
}
