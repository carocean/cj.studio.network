package cj.studio.network.peer;


import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.ServiceCollection;
import cj.studio.network.NetworkFrame;
import cj.studio.network.peer.connection.TcpConnection;
import cj.ultimate.util.StringUtil;

import java.util.HashMap;
import java.util.Map;

public class Peer implements IPeer {
    private String peerName;
    IConnection connection;
    INetworkPeerContainer container;
    IServiceProvider site;

    private Peer(String peerName, IServiceProvider parent) {
        this.peerName = peerName;
        site = new PeerServiceSite(parent);

    }

    public static IPeer create(String peerName, IServiceProvider parent) {
        Peer peer = new Peer(peerName, parent);

        return peer;
    }



    @Override
    public String getNodeHost() {
        return connection.getHost();
    }

    @Override
    public String getNodeProtocol() {
        return connection.getProtocol();
    }

    @Override
    public int getNodePort() {
        return connection.getPort();
    }

    @Override
    public String peerName() {
        return peerName;
    }

    @Override
    public IMasterNetworkPeer connect(String networkNode, String authmode, String user, String token, String masterNetowrkName,IOnerror onerror, IOnopen onopen, IOnmessage onmessage, IOnclose onclose) {
        container = new NetworkPeerContainer(masterNetowrkName,site);
        int pos = networkNode.indexOf("://");
        if (pos < 0) {
            throw new EcmException("地址格式错误:" + networkNode);
        }
        String protocol = networkNode.substring(0, pos);
        String remain = networkNode.substring(pos + "://".length(), networkNode.length());
        pos = remain.indexOf(":");
        int port = 0;
        String ip = "";
        Map<String, String> props = new HashMap<>();
        if (pos < 0) {
            ip = remain;
            port = 80;
        } else {
            ip = remain.substring(0, pos);
            remain = remain.substring(pos + 1, remain.length());
            pos = remain.indexOf("?");
            if (pos < 0) {
                String strPort = remain;
                port = Integer.valueOf(strPort);
            } else {
                String strPort = remain.substring(0, pos);
                port = Integer.valueOf(strPort);
                remain = remain.substring(pos + 1, remain.length());
                parseProps(remain, props);
            }
        }

        switch (protocol) {
            case "tcp":
                doTcpConnection(protocol, ip, port, props);
                break;
            default:
                throw new EcmException("不支持的连接协议:" + protocol);
        }
        return listenManagerNetwork(authmode, user, token, masterNetowrkName,onerror, onopen, onmessage, onclose);
    }

    private IMasterNetworkPeer listenManagerNetwork(String authmode, String user, String token, String managerNetowrkName,IOnerror onerror, IOnopen onopen, IOnmessage onmessage, IOnclose onclose) {
        IMasterNetworkPeer manager =(IMasterNetworkPeer) listen(managerNetowrkName,onerror, onopen, onmessage, onclose);
        NetworkFrame frame = new NetworkFrame(String.format("auth / network/1.0"));
        frame.head("Auth-User", user);
        frame.head("Auth-Mode", authmode);
        frame.head("Auth-Token", token);
        frame.head("Peer-Name", peerName);
        manager.send(frame);
        return manager;
    }

    private void doTcpConnection(String protocol, String ip, int port, Map<String, String> props) {
        connection = new TcpConnection(site);
        connection.connect(protocol, ip, port, props);
    }

    private void parseProps(String queryString, Map<String, String> props) {
        String[] arr = queryString.split("&");
        for (String pair : arr) {
            if (StringUtil.isEmpty(pair)) {
                continue;
            }
            String[] e = pair.split("=");
            String key = e[0];
            String v = "";
            if (e.length > 1) {
                v = e[1];
            }
            props.put(key, v);
        }
    }


    @Override
    public INetworkPeer listen(String networkName,IOnerror onerror, IOnopen onopen, IOnmessage onmessage, IOnclose onclose) {
        if (container.exists(networkName)) {
            throw new EcmException("已侦听网络：" + networkName);
        }

        INetworkPeer networkPeer = container.create(connection, networkName,onerror, onopen, onmessage, onclose);
        NetworkFrame frame = new NetworkFrame("listenNetwork / network/1.0");
        frame.head("Peer-Name", peerName);
        networkPeer.send(frame);
        return networkPeer;
    }

    @Override
    public void close() {
        container.dispose();
        connection.close();
    }

    @Override
    public IServiceProvider site() {
        return site;
    }

    class DefaultManagerNetworkOnmessage implements IOnmessage {
        IOnmessage userOnmessage;

        public DefaultManagerNetworkOnmessage(IOnmessage userOnmessage) {
            this.userOnmessage = userOnmessage;
        }

        @Override
        public void onmessage(NetworkFrame frame, IServiceProvider site) {
            if (userOnmessage != null) {
                userOnmessage.onmessage(frame, site);
            }
        }
    }

    class PeerServiceSite implements IServiceProvider {
        IServiceProvider parent;

        public PeerServiceSite(IServiceProvider parent) {
            this.parent = parent;
        }

        @Override
        public <T> ServiceCollection<T> getServices(Class<T> serviceClazz) {

            if (parent != null) {
                return parent.getServices(serviceClazz);
            }
            return null;
        }

        @Override
        public Object getService(String serviceId) {
            if ("$.peer".equals(serviceId)) {
                return Peer.this;
            }
            if ("$.peer.container".equals(serviceId)) {
                return container;
            }
            if ("$.peer.name".equals(serviceId)) {
                return peerName;
            }

            if (parent != null) {
                return parent.getService(serviceId);
            }
            return null;
        }
    }
}
