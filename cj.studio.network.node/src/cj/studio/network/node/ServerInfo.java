package cj.studio.network.node;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.ultimate.util.StringUtil;

import java.util.HashMap;
import java.util.Map;

public class ServerInfo {
    String protocol;
    String host;
    int port;
    Map<String,String> props;

    @Override
    public String toString() {
        return String.format("%s://%s:%s",protocol,host,port);
    }

    public void parse(Map<String, Object> node) {
        props=new HashMap<>();
        Map<String, Object> server = (Map<String, Object>) node.get("server");
        if (server == null) {
            throw new EcmException(String.format("缺少server配置，在文件：node.yaml"));
        }
        String h = (String) server.get("host");
        if (StringUtil.isEmpty(h)) {
            throw new EcmException(String.format("server配置缺少host，在文件：node.yaml"));
        }
        int pos = h.indexOf("://");
        if (pos < 0) {
            throw new EcmException(String.format("host地址格式错误，应为：protcol://domain:port，在文件：node.yaml"));
        }
        protocol = h.substring(0, pos);
        String remain = h.substring(pos + "://".length(), h.length());
        pos = remain.indexOf(":");
        if (pos < 0) {
            port = 80;
            CJSystem.logging().info("server使用默认端口：" + port);
            host = remain;
            parseProps(server);
            return;
        }
        host = remain.substring(0, pos);
        String p = remain.substring(pos + 1, remain.length());
        port = Integer.valueOf(p);
        parseProps(server);
    }

    private void parseProps(Map<String, Object> server) {
       Map<String,String> props= (Map<String, String>) server.get("props");
       this.props.putAll(props);
       props.clear();
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public Map<String, String> getProps() {
        return props;
    }
}
