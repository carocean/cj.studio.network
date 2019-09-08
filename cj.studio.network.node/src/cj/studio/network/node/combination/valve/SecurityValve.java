package cj.studio.network.node.combination.valve;

import cj.studio.ecm.net.CircuitException;
import cj.studio.network.IAccessControllerStrategy;
import cj.studio.network.INetwork;
import cj.studio.network.NetworkFrame;
import cj.studio.network.UserPrincipal;
import cj.studio.network.node.INetworkContainer;
import cj.studio.network.node.INetworkNodeAppManager;
import cj.studio.util.reactor.Event;
import cj.studio.util.reactor.IPipeline;
import cj.studio.util.reactor.IValve;
import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

public class SecurityValve implements IValve {
    INetworkContainer container;
    IAccessControllerStrategy accessControllerStrategy;

    public SecurityValve(INetworkContainer container, INetworkNodeAppManager appManager) {
        this.container = container;
        accessControllerStrategy = appManager.createAccessControllerStrategy();
    }


    @Override
    public void flow(Event e, IPipeline pipeline) throws CircuitException {
        //开放主网络的listenNetwork,auth指令，其它主网络指令及工作网络指令均需检查peer会话,如果不存在会话则关闭channel
        NetworkFrame frame = (NetworkFrame) e.getParameters().get("frame");
        Channel channel = (Channel) e.getParameters().get("channel");
        if (pipeline.key().equals(container.getMasterNetworkName())) {
            switch (frame.command()) {//这两个指令放过去
                case "listenNetwork":
                case "auth":
                    pipeline.nextFlow(e, this);
                    return;
            }
        }
        //检查会话是否存在
        Attribute<UserPrincipal> attribute = channel.attr(AttributeKey.valueOf("Peer-UserPrincipal"));
        if (attribute != null && attribute.get() != null) {
            UserPrincipal userPrincipal = attribute.get();
            try {
                if (accessControllerStrategy != null) {
                    INetwork network = container.getNetwork(pipeline.key());
                    accessControllerStrategy.access(frame, userPrincipal, network);
                }
            } catch (Throwable throwable) {
                CircuitException ce = CircuitException.search(throwable);
                if (ce != null) {
                    nextError(e, ce, pipeline);
                } else {
                    nextError(e, new CircuitException("803", throwable), pipeline);
                }
                channel.close();//可能没权限访问故而关闭
                return;
            }
            pipeline.nextFlow(e, this);//存在会话，放过去
            return;
        }
        //不存在会话，关闭channel，在关闭前发送错误信息
        nextError(e, new CircuitException("802", "没有登录，服务器拒绝服务。"), pipeline);
        channel.close();
    }

    @Override
    public void nextError(Event e, Throwable error, IPipeline pipeline) throws CircuitException {
        pipeline.nextError(e, error, this);
    }

}
