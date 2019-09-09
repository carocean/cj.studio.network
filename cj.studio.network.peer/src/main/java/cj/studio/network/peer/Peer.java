package cj.studio.network.peer;


import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.ServiceCollection;
import cj.studio.network.NetworkFrame;
import cj.studio.network.peer.connection.IOnReconnectEvent;
import cj.studio.network.peer.connection.TcpConnection;
import cj.ultimate.util.StringUtil;

import java.util.HashMap;
import java.util.Map;

public class Peer implements IPeer {
    private String peerName;
    IConnection connection;
    INetworkPeerContainer container;
    IServiceProvider site;
    IOnReconnectEvent onReconnectEvent;

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
    public void connect(String networkNodeAddress, String masterNetowrkName, IOnReconnectEvent reconnectEvent) {
        container = new NetworkPeerContainer(masterNetowrkName, site);
        int pos = networkNodeAddress.indexOf("://");
        if (pos < 0) {
            throw new EcmException("地址格式错误:" + networkNodeAddress);
        }
        String protocol = networkNodeAddress.substring(0, pos);
        String remain = networkNodeAddress.substring(pos + "://".length(), networkNodeAddress.length());
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
        this.onReconnectEvent = new DefaultOnReconnectEvent(reconnectEvent);
        switch (protocol) {
            case "tcp":
                connection = new TcpConnection(onReconnectEvent, site);
                connection.connect(protocol, ip, port, props);
                break;
            default:
                throw new EcmException("不支持的连接协议:" + protocol);
        }
    }

    @Override
    public void connect(String networkNodeAddress, String masterNetowrkName) {
        connect(networkNodeAddress, masterNetowrkName, null);
    }

    @Override
    public IMasterNetworkPeer auth(String authmode, String user, String token, IOnerror onerror, IOnopen onopen, IOnmessage onmessage, IOnclose onclose) {
        onReconnectEvent.init(authmode, user, token, onerror, onopen, onmessage, onclose);
        IMasterNetworkPeer manager = (IMasterNetworkPeer) listen(container.getMasterNetowrkName(), onerror, onopen, onmessage, onclose);
        NetworkFrame frame = new NetworkFrame(String.format("auth / network/1.0"));
        frame.head("Auth-User", user);
        frame.head("Auth-Mode", authmode);
        frame.head("Auth-Token", token);
        frame.head("Peer-Name", peerName);
        manager.send(frame);
        return manager;
    }

    @Override
    public INetworkPeer listen(String networkName, IOnerror onerror, IOnopen onopen, IOnmessage onmessage, IOnclose onclose) {
        if (container.exists(networkName)) {
            throw new EcmException("已侦听网络：" + networkName);
        }

        INetworkPeer networkPeer = container.create(connection, networkName, onerror, onopen, onmessage, onclose);
        NetworkFrame frame = new NetworkFrame("listenNetwork / network/1.0");
        frame.head("Peer-Name", peerName);
        networkPeer.send(frame);
        return networkPeer;
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
    public void close() {
        container.dispose();
        connection.close();
    }

    @Override
    public IServiceProvider site() {
        return site;
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
            if ("$.peer.channel".equals(serviceId)) {
                return connection.getService("$.channel");
            }
            if (parent != null) {
                return parent.getService(serviceId);
            }
            return null;
        }
    }

    class DefaultOnReconnectEvent implements IOnReconnectEvent {
        String authmode;
        String user;
        String token;
        IOnerror onerror;
        IOnopen onopen;
        IOnmessage onmessage;
        IOnclose onclose;
        IOnReconnectEvent onReconnectEvent;

        public DefaultOnReconnectEvent() {
        }

        public DefaultOnReconnectEvent(IOnReconnectEvent onReconnectEvent) {
            this.onReconnectEvent = onReconnectEvent;
        }

        @Override
        public void onreconnect() {
            Peer peer = Peer.this;
            peer.container.remove(peer.container.getMasterNetwork());//移除主网重新侦听
            peer.auth(authmode, user, token, onerror, onopen, onmessage, onclose);
            //将已有的网络重新恢复立侦听
            relistenNetwork(container);
            if (onReconnectEvent != null) {
                onReconnectEvent.onreconnect();
            }
        }

        private void relistenNetwork(INetworkPeerContainer container) {
            String[] names = container.enumNetworkName();
            for (String name : names) {
                INetworkPeer networkPeer = container.get(name);
                NetworkFrame frame = new NetworkFrame("listenNetwork / network/1.0");
                frame.head("Peer-Name", peerName);
                networkPeer.send(frame);
            }
        }

        @Override
        public void init(String authmode, String user, String token, IOnerror onerror, IOnopen onopen, IOnmessage onmessage, IOnclose onclose) {
            this.authmode = authmode;
            this.user = user;
            this.token = token;
            this.onerror = onerror;
            this.onopen = onopen;
            this.onmessage = onmessage;
            this.onclose = onclose;
            if (onReconnectEvent != null) {
                onReconnectEvent.init(authmode, user, token, onerror, onopen, onmessage, onclose);
            }
        }
    }

}
