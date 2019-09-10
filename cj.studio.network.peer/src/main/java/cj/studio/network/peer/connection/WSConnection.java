package cj.studio.network.peer.connection;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.net.util.TcpFrameBox;
import cj.studio.network.NetworkFrame;
import cj.studio.network.PackFrame;
import cj.studio.network.peer.IConnection;
import cj.studio.network.util.PropUtil;
import cj.ultimate.util.StringUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleStateHandler;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class WSConnection implements IConnection, IServiceProvider {
    private final IOnReconnectEvent onReconnectEvent;
    EventLoopGroup exepool;
    private Channel channel;
    IServiceProvider site;
    String peerName;
    private String protocol;
    private String host;
    private int port;
    private long heartbeat;
    private Map<String, String> props;
    private long reconnect_times;
    private long reconnect_interval;
    private int workThreadCount;
    private boolean forbiddenReconnect;
    private String wspath;
    int maxContentLength;

    public WSConnection(IOnReconnectEvent onReconnectEvent, IServiceProvider site) {
        this.onReconnectEvent = onReconnectEvent;
        this.site = site;
        peerName = (String) site.getService("$.peer.name");
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public <T> ServiceCollection<T> getServices(Class<T> serviceClazz) {
        return site != null ? site.getServices(serviceClazz) : null;
    }

    @Override
    public Object getService(String serviceId) {
        if ("$.prop.heartbeat".equals(serviceId)) {
            return heartbeat;
        }
        if ("$.prop.reconnect_times".equals(serviceId)) {
            return reconnect_times;
        }
        if ("$.prop.reconnect_interval".equals(serviceId)) {
            return reconnect_interval;
        }
        if ("$.connection".equals(serviceId)) {
            return this;
        }
        if ("$.channel".equals(serviceId)) {
            return channel;
        }
        return site != null ? site.getService(serviceId) : null;
    }

    @Override
    public void forbiddenReconnect() {
        this.forbiddenReconnect = true;
    }

    @Override
    public boolean isForbiddenReconnect() {
        return forbiddenReconnect;
    }

    @Override
    public void reconnect() {
        if (this.forbiddenReconnect) {
            return;
        }
        if (exepool != null) {
            if (!exepool.isShutdown() && !exepool.isTerminated()) {
                exepool.shutdownGracefully();
            }
            exepool = null;
        }
        Map<String, String> map = new HashMap<>();
        if (props != null) {
            map.putAll(props);
            props.clear();
        }
        connect(protocol, host, port, map);
        if (onReconnectEvent != null) {
            onReconnectEvent.onreconnect();
        }
    }

    @Override
    public void connect(String protocol, String ip, int port, Map<String, String> props) {
        this.protocol = protocol;
        this.host = ip;
        this.port = port;
        this.props = props;
        parseProps(props);

        EventLoopGroup group = null;
        if (workThreadCount < 1) {
            group = new NioEventLoopGroup();
        } else {
            group = new NioEventLoopGroup(workThreadCount);
        }

        Bootstrap b = new Bootstrap();
        URI uri = null;
        String url = String.format("%s://%s:%s%s", this.protocol, this.host, this.port, this.wspath);
        try {
            uri = new URI(url);
        } catch (URISyntaxException e1) {
            e1.printStackTrace();
        }
        CJSystem.logging().info(getClass(), "连接地址：" + url);
        HttpHeaders customHeaders = new DefaultHttpHeaders();
//		customHeaders.add("MyHeader", "MyValue");
        final WSClientHandler handler =
                new WSClientHandler(
                        WebSocketClientHandshakerFactory.newHandshaker(
                                uri, WebSocketVersion.V13, null, false, customHeaders), this);

        try {
            b.group(group).channel(NioSocketChannel.class).handler(new WebsocketClientGatewaySocketInitializer("wss".equals(protocol), handler));
            this.channel = b.connect(ip, port).sync().channel();
            handler.handshakeFuture().sync();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private void parseProps(Map<String, String> props) {
        String wspath = PropUtil.getValue(props.get("wspath"));
        if (StringUtil.isEmpty(wspath)) {
            wspath = "/websocket";
        }
        if (!wspath.startsWith("/")) {
            wspath = "/" + wspath;
        }
        this.wspath = wspath;
        String strheartbeat = PropUtil.getValue(props
                .get("heartbeat"));
        if (StringUtil.isEmpty(strheartbeat)) {
            strheartbeat = "0";
        }
        this.heartbeat = Long.valueOf(strheartbeat);
        //maxContentLength
        String strmaxContentLength = PropUtil.getValue(props
                .get("maxContentLength"));
        if (StringUtil.isEmpty(strmaxContentLength)) {
            strmaxContentLength = "2097152";
        }
        this.maxContentLength = Integer.valueOf(strmaxContentLength);

        String strreconnect_times = PropUtil.getValue(props
                .get("reconnect_times"));
        if (StringUtil.isEmpty(strreconnect_times)) {
            strreconnect_times = "0";
        }
        this.reconnect_times = Long.valueOf(reconnect_times);

        String strreconnect_interval = PropUtil.getValue(props
                .get("reconnect_interval"));
        if (StringUtil.isEmpty(strreconnect_interval)) {
            strreconnect_interval = "5000";
        }
        this.reconnect_interval = Long.valueOf(strreconnect_interval);

        String workThreadCount = PropUtil.getValue(props
                .get("workThreadCount"));
        if (StringUtil.isEmpty(workThreadCount)) {
            workThreadCount = "0";
        }
        this.workThreadCount = Integer.valueOf(workThreadCount);

        CJSystem.logging().info(getClass(), String.format("连接属性：workThreadCount=%s,heartbeat=%s,reconnect_times=%s,reconnect_interval=%s",
                workThreadCount, heartbeat, reconnect_times, reconnect_interval));

    }

    @Override
    public void send(NetworkFrame frame) {
        frame.head("Peer-Name", peerName);
        ByteBuf bb = Unpooled.buffer();
        byte[] b = frame.toBytes();
        frame.dispose();
        bb.writeBytes(b, 0, b.length);
        BinaryWebSocketFrame webSocketFrame = new BinaryWebSocketFrame(bb);
        channel.writeAndFlush(webSocketFrame);
    }

    @Override
    public boolean isConnected() {
        return channel.isWritable();
    }

    @Override
    public void close() {
        channel.close();
    }

    class WebsocketClientGatewaySocketInitializer extends ChannelInitializer<SocketChannel> {
        WSClientHandler handler;
        SslContext sslCtx;

        public WebsocketClientGatewaySocketInitializer(boolean SSL, WSClientHandler handler) throws SSLException {
            this.handler = handler;
            if (SSL) {
                sslCtx = SslContext.newClientContext(InsecureTrustManagerFactory.INSTANCE);
            }
        }

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            if (sslCtx != null) {
                pipeline.addLast(sslCtx.newHandler(ch.alloc(), host, port));
            }
            pipeline.addLast(new HttpClientCodec());
            pipeline.addLast(new WebSocketClientCompressionHandler());
            if (heartbeat > 0) {
                pipeline.addLast(new IdleStateHandler(0, 0, heartbeat, TimeUnit.SECONDS));
            }
            pipeline.addLast(new HttpObjectAggregator(maxContentLength));
            pipeline.addLast(handler);
        }

    }


}
