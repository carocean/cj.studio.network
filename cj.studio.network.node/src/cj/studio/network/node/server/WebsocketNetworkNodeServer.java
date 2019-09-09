package cj.studio.network.node.server;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.ServiceCollection;
import cj.studio.network.node.INetworkNodeConfig;
import cj.studio.network.node.INetworkNodeServer;
import cj.studio.network.node.ReactorInfo;
import cj.studio.network.node.ServerInfo;
import cj.studio.network.node.combination.ReactorPipelineCombination;
import cj.studio.network.node.server.initializer.TcpChannelInitializer;
import cj.studio.network.node.server.initializer.WebsocketChannelInitializer;
import cj.studio.util.reactor.*;
import cj.ultimate.util.StringUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.internal.SystemPropertyUtil;

public class WebsocketNetworkNodeServer implements INetworkNodeServer, IServiceProvider {
    IServiceProvider site;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    boolean isStarted;
    private int bossThreadCount;
    private int workThreadCount;
    private ServerInfo serverInfo;
    private long heartbeat;
    private IReactor reactor;
    private long overtimes;
    private long maxContentLength;
    private String wspath;



    public WebsocketNetworkNodeServer(IServiceProvider site) {
        this.site = site;

    }


    @Override
    public <T> ServiceCollection<T> getServices(Class<T> serviceClazz) {
        return null;
    }

    @Override
    public Object getService(String serviceId) {
        if ("$.reactor".equals(serviceId)) {
            return reactor;
        }
        if ("$.server.info".equals(serviceId)) {
            return serverInfo;
        }
        if ("$.server.heartbeat".equals(serviceId)) {
            return heartbeat;
        }
        if ("$.server.maxContentLength".equals(serviceId)) {
            return maxContentLength;
        }
        if ("$.server.wspath".equals(serviceId)) {
            return wspath;
        }
        if ("$.server.overtimes".equals(serviceId)) {
            return overtimes;
        }
        return site.getService(serviceId);
    }

    @Override
    public void stop() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        isStarted = false;
        site = null;
    }

    @Override
    public void start() {
        INetworkNodeConfig config = (INetworkNodeConfig) site.getService("$.network.config");
        this.serverInfo = config.getServerInfo();
        if (isStarted) {
            throw new EcmException(String.format("服务器:%s已启动", serverInfo));
        }
        String strbossThreadCount = serverInfo.getProps().get("bossThreadCount") == null ? "" : serverInfo.getProps().get("bossThreadCount") + "";
        this.bossThreadCount = StringUtil.isEmpty(strbossThreadCount) || "0".equals(strbossThreadCount) ? 1 : Integer.valueOf(strbossThreadCount);
        String strworkThreadCount = serverInfo.getProps().get("workThreadCount") == null ? "" : serverInfo.getProps().get("workThreadCount") + "";
        this.workThreadCount = StringUtil.isEmpty(strworkThreadCount) || "0".equals(strworkThreadCount) ? Math.max(1, SystemPropertyUtil.getInt(
                "io.netty.eventLoopThreads", Runtime.getRuntime().availableProcessors() * 2)) : Integer.valueOf(strworkThreadCount);

        this.heartbeat = 0;
        if (serverInfo.getProps().get("heartbeat") != null) {
            this.heartbeat = serverInfo.getProps().get("heartbeat") instanceof Integer ? (int) serverInfo.getProps().get("heartbeat") : (long) serverInfo.getProps().get("heartbeat");
        }
        this.maxContentLength = 0;
        if (serverInfo.getProps().get("maxContentLength") != null) {
            this.maxContentLength = serverInfo.getProps().get("maxContentLength") instanceof Integer ? (int) serverInfo.getProps().get("maxContentLength") : (long) serverInfo.getProps().get("maxContentLength");
        }
        if (maxContentLength < 1) {
            maxContentLength = 2 * 1024 * 1024;
        }
        wspath = (String) serverInfo.getProps().get("wspath");
        if (StringUtil.isEmpty(wspath)) {
            wspath = "/";
        }
        CJSystem.logging().info(getClass(), String.format("服务路径：wspath=%s", wspath));
        this.overtimes = 0;
        if (serverInfo.getProps().get("overtimes") != null) {
            this.overtimes = serverInfo.getProps().get("overtimes") instanceof Integer ? (int) serverInfo.getProps().get("overtimes") : (long) serverInfo.getProps().get("overtimes");
        }
        if (this.heartbeat > 0) {
            CJSystem.logging().info(getClass(), String.format("开启了心跳，策略：heartbeat=%s,overtimes=%s", heartbeat, overtimes));
        }

        startRactor(config);
        bossGroup = new NioEventLoopGroup(bossThreadCount);
        workerGroup = new NioEventLoopGroup(workThreadCount);
        ServerBootstrap b = new ServerBootstrap();
        try {
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new WebsocketChannelInitializer(this));

            // Bind and start to accept incoming connections.
            Channel ch = null;
            if ("localhost".equals(serverInfo.getHost())) {
                ch = b.bind(serverInfo.getPort()).sync().channel();
            } else {
                ch = b.bind(serverInfo.getHost(), serverInfo.getPort()).sync().channel();
            }
            ch.closeFuture();// .sync();
            isStarted = true;

        } catch (InterruptedException e) {
            throw new EcmException(e);
        }
    }

    protected void startRactor(INetworkNodeConfig config) {
        ReactorInfo reactorInfo = config.getReactorInfo();
        int workThreadCount = reactorInfo.workThreadCount();
        int capacity = reactorInfo.queueCapacity();
        IPipelineCombination combination = new ReactorPipelineCombination(site);

        reactor = Reactor.open(DefaultReactor.class, workThreadCount, capacity, combination, new ReactorServiceProvider(this));
    }


    class ReactorServiceProvider implements IServiceProvider {
        private final IServiceProvider parent;

        public ReactorServiceProvider(IServiceProvider parent) {
            this.parent = parent;
        }

        @Override
        public <T> T getService(String name) {
            return (T) parent.getService(name);
        }

        @Override
        public <T> ServiceCollection<T> getServices(Class<T> clazz) {
            return parent.getServices(clazz);
        }
    }
}
