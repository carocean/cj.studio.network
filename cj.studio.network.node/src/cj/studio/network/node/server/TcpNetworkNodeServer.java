package cj.studio.network.node.server;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.ServiceCollection;
import cj.studio.network.node.INetworkNodeConfig;
import cj.studio.network.node.INetworkNodeServer;
import cj.studio.network.node.ReactorInfo;
import cj.studio.network.node.ServerInfo;
import cj.studio.network.node.combination.ReactorPipelineCombination;
import cj.studio.network.node.server.initializer.TcpChannelInitializer;
import cj.studio.util.reactor.*;
import cj.ultimate.util.StringUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.internal.SystemPropertyUtil;

public class TcpNetworkNodeServer implements INetworkNodeServer, IServiceProvider {
    IServiceProvider site;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    boolean isStarted;
    private int bossThreadCount;
    private int workThreadCount;
    private ServerInfo serverInfo;
    private long heartbeat;
    private IReactor reactor;

    public TcpNetworkNodeServer(IServiceProvider site) {
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

        String interval = StringUtil.isEmpty((String) serverInfo.getProps().get("heartbeat")) ? "0" : serverInfo.getProps().get("heartbeat") + "";
        this.heartbeat = Long.valueOf(interval);

        startRactor(config);
        bossGroup = new NioEventLoopGroup(bossThreadCount);
        workerGroup = new NioEventLoopGroup(workThreadCount);
        ServerBootstrap b = new ServerBootstrap();
        try {
            b.group(bossGroup, workerGroup).childOption(ChannelOption.SO_KEEPALIVE, true)
                    .channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new TcpChannelInitializer(this));

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


    class ReactorServiceProvider implements cj.studio.util.reactor.IServiceProvider {
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
