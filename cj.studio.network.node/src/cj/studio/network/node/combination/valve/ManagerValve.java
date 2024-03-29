package cj.studio.network.node.combination.valve;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.net.CircuitException;
import cj.studio.network.IAuthenticateStrategy;
import cj.studio.network.UserPrincipal;
import cj.studio.network.NetworkCircuit;
import cj.studio.network.NetworkFrame;
import cj.studio.network.INetwork;
import cj.studio.network.node.INetworkNodeAppManager;
import cj.studio.network.node.INetworkContainer;
import cj.studio.network.node.INetworkNodeConfig;
import cj.studio.util.reactor.Event;
import cj.studio.util.reactor.IPipeline;
import cj.studio.util.reactor.IValve;
import cj.ultimate.gson2.com.google.gson.Gson;
import cj.ultimate.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 处理管理网络逻辑
 */
public class ManagerValve implements IValve {
    INetworkContainer container;

    public ManagerValve(INetworkContainer container) {
        this.container = container;
    }

    @Override
    public void nextError(Event e, Throwable error, IPipeline pipeline) throws CircuitException {
        pipeline.nextError(e, error, this);
    }

    @Override
    public void flow(Event e, IPipeline pipeline) throws CircuitException {
        if (!container.getMasterNetworkName().equals(pipeline.key())) {//不是管理网络则放过去
            NetworkFrame frame = (NetworkFrame) e.getParameters().get("frame");
            Channel channel = (Channel) e.getParameters().get("channel");
            if ("NETWORK/1.0".equals(frame.protocol())) {//属于系统指令，改道主网络处理，且不再经由后续的管道，即不对开发者呈现系统指令
                if ("listenNetwork".equals(frame.command())) {
                    listenNetwork(e, pipeline);
                    return;
                }
                if ("byeNetwork".equals(frame.command())) {
                    byeNetwork(e, pipeline);
                    return;
                }
                if ("infoNetwork".equals(frame.command())) {
                    infoNetwork(e, pipeline);
                    return;
                }
            }
            pipeline.nextFlow(e, this);
            return;
        }
        switch (e.getCmd()) {
            case "listenNetwork":
                listenNetwork(e, pipeline);
                break;
            case "auth":
                authManagerNetwork(e, pipeline);
                break;
            case "infoNetwork":
                infoNetwork(e, pipeline);
                break;
            case "createNetwork":
                createNetwork(e, pipeline);
                break;
            case "listNetwork":
                listNetwork(e, pipeline);
                break;
            case "existsNetwork":
                existsNetwork(e, pipeline);
                break;
            case "removeNetwork":
                removeNetwork(e, pipeline);
                break;
            case "renameNetwork":
                renameNetwork(e, pipeline);
                break;
            case "changeCastmode":
                changeCastmode(e, pipeline);
                break;
            default:
                pipeline.nextError(e, new CircuitException("501", String.format("The Command %s is not supported", e.getCmd())), this);
                break;
        }
    }

    private void byeNetwork(Event e, IPipeline pipeline) throws CircuitException {
        NetworkFrame frame = (NetworkFrame) e.getParameters().get("frame");
        Channel channel = (Channel) e.getParameters().get("channel");

        ByteBuf bb = Unpooled.buffer();
        NetworkFrame f = new NetworkFrame(frame.toString(), bb);

        NetworkCircuit c = new NetworkCircuit("network/1.0 200 ok");
        c.head("Network-Name", frame.rootName());
        bb.writeBytes(c.toByteBuf());
        INetwork network = (INetwork) container.getMasterNetwork();//改道主网络发送,因为系统消息不需要广播
        network.cast(channel, f);
        if (!pipeline.key().equals(network.getInfo().getName())) {
            network = container.getNetwork(pipeline.key());//直接移除
            network.removeChannel(channel);
        }
    }


    private void listenNetwork(Event e, IPipeline pipeline) throws CircuitException {
        NetworkFrame frame = (NetworkFrame) e.getParameters().get("frame");
        Channel channel = (Channel) e.getParameters().get("channel");
        INetwork network = (INetwork) container.getNetwork(pipeline.key());
        if (!network.existsChannel(channel)) {
            network.addChannel(channel);
        }

        ByteBuf bb = Unpooled.buffer();
        NetworkFrame f = new NetworkFrame(frame.toString(), bb);

        NetworkCircuit c = new NetworkCircuit("network/1.0 200 ok");
        c.head("Network-Name", frame.rootName());
        bb.writeBytes(c.toByteBuf());
        network = (INetwork) container.getMasterNetwork();//改道主网络发送,因为系统消息不需要广播
        network.cast(channel, f);
    }

    private void authManagerNetwork(Event e, IPipeline pipeline) throws CircuitException {
        NetworkFrame frame = (NetworkFrame) e.getParameters().get("frame");
        Channel channel = (Channel) e.getParameters().get("channel");
        ByteBuf bb = Unpooled.buffer();
        NetworkFrame f = new NetworkFrame(frame.toString(), bb);
        String authUser = frame.head("Auth-User");
        if (StringUtil.isEmpty(authUser)) {
            pipeline.nextError(e, new CircuitException("404", String.format("The Auth-User Of Header is Null.")), this);
            channel.close();
            return;
        }
        String authMode = frame.head("Auth-Mode");
        if (StringUtil.isEmpty(authMode)) {
            pipeline.nextError(e, new CircuitException("404", String.format("The Auth-Mode Of Header is Null.")), this);
            channel.close();
            return;
        }
        String authToken = frame.head("Auth-Token");
        if (StringUtil.isEmpty(authToken)) {
            pipeline.nextError(e, new CircuitException("404", String.format("The Auth-Token Of Header is Null.")), this);
            channel.close();
            return;
        }
        String peerName = frame.head("Peer-Name");
        if (StringUtil.isEmpty(peerName)) {
            pipeline.nextError(e, new CircuitException("404", String.format("The Peer-Name Of Header is Null.")), this);
            channel.close();
            return;
        }
        AttributeKey<String> peerNameKey = AttributeKey.valueOf("Peer-Name");
        channel.attr(peerNameKey).set(peerName);

        INetworkNodeAppManager appManager = (INetworkNodeAppManager) pipeline.site().getService("$.network.app.manager");

        UserPrincipal userPrincipal = null;
        if (appManager.isEnableRBAC()) {
            INetwork network=container.getNetwork(pipeline.key());
            try {
                IAuthenticateStrategy authenticateStrategy = appManager.createAuthenticateStrategy(authMode,network);//创建认证策略
                if (authenticateStrategy == null) {
                    throw new CircuitException("801", "认证失败，缺少对应的认证策略：" + authMode);
                }
                userPrincipal = authenticateStrategy.authenticate(authUser, authToken);//认证
                if (userPrincipal == null) {
                    throw new CircuitException("801", "认证失败，返回的userPrincipal为空");
                }
            } catch (Throwable throwable) {
                CJSystem.logging().error(getClass(),throwable);
                CircuitException ce = CircuitException.search(throwable);
                if (ce != null) {
                    pipeline.nextError(e, ce, this);
                } else {
                    pipeline.nextError(e, new CircuitException("801", String.format("Fail to Auth.The Peer was Closed. Cause by %s", throwable.getMessage())), this);
                }
                channel.close();//认证不通过则关闭对端
                return;
            }
            AttributeKey<UserPrincipal> userPrincipalAttributeKey = AttributeKey.valueOf("Peer-UserPrincipal");
            channel.attr(userPrincipalAttributeKey).set(userPrincipal);
        }
        //认证通过返回会话，会话包将用户及角色，并将会话赋予channel,之后将验证channel是否有会话，无会话则关闭channel
        NetworkCircuit c = new NetworkCircuit("network/1.0 200 ok");
        if (userPrincipal != null) {
            c.content().writeBytes(new Gson().toJson(userPrincipal).getBytes());
        }
        bb.writeBytes(c.toByteBuf());
        INetwork network = (INetwork) container.getMasterNetwork();//改道主网络发送,因为系统消息不需要广播
        network.cast(channel, f);
    }


    private void infoNetwork(Event e, IPipeline pipeline) throws CircuitException {
        NetworkFrame frame = (NetworkFrame) e.getParameters().get("frame");
        Channel channel = (Channel) e.getParameters().get("channel");
        ByteBuf bb = Unpooled.buffer();
        NetworkFrame f = new NetworkFrame(frame.toString(), bb);
        String networkName = frame.head("Network-Name");
        if (StringUtil.isEmpty(networkName)) {
            pipeline.nextError(e, new CircuitException("404", String.format("The Network-Name Of Header is Null.")), this);
            return;
        }

        NetworkCircuit c = new NetworkCircuit("network/1.0 200 ok");
        if (!container.existsNetwork(networkName)) {
            c.status("404");
            c.message(String.format("The Network %s is Not Exists.", networkName));
        } else {
            c.status("200");
            c.message("ok");
            INetwork network = container.getNetwork(networkName);
            c.content().writeBytes(new Gson().toJson(network.getInfo()).getBytes());
        }
        c.head("Network-Name", networkName);
        bb.writeBytes(c.toByteBuf());
        INetwork network = (INetwork) container.getMasterNetwork();//改道主网络发送,因为系统消息不需要广播
        network.cast(channel, f);
    }

    private void changeCastmode(Event e, IPipeline pipeline) throws CircuitException {
        NetworkFrame frame = (NetworkFrame) e.getParameters().get("frame");
        Channel channel = (Channel) e.getParameters().get("channel");
        ByteBuf bb = Unpooled.buffer();
        NetworkFrame f = new NetworkFrame(frame.toString(), bb);
        String networkName = frame.head("Network-Name");
        if (StringUtil.isEmpty(networkName)) {
            pipeline.nextError(e, new CircuitException("404", String.format("The Network-Name Of Header is Null.")), this);
            return;
        }
        String castmode = frame.head("Network-Castmode");
        if (StringUtil.isEmpty(castmode)) {
            pipeline.nextError(e, new CircuitException("404", String.format("The Network-Castmode Of Header is Null.")), this);
            return;
        }
        if (!container.existsNetwork(networkName)) {
            pipeline.nextError(e, new CircuitException("404", frame.head(String.format("The Network %s is not Exists.", networkName))), this);
            return;
        }
        container.changeNetworkCastmode(networkName, castmode);
        NetworkCircuit c = new NetworkCircuit("network/1.0 200 ok");
        c.head("status", "200");
        c.head("message", String.format("The Castmode Of Network %s was change.", networkName));
        c.head("Network-Name", networkName);
        c.head("Old-Network-Castmode", container.getNetwork(networkName).getInfo().getCastmode());
        c.head("New-Network-Castmode", networkName);
        bb.writeBytes(c.toByteBuf());

        INetwork network = (INetwork) container.getMasterNetwork();//改道主网络发送,因为系统消息不需要广播
        network.cast(channel, f);
    }

    private void renameNetwork(Event e, IPipeline pipeline) throws CircuitException {
        NetworkFrame frame = (NetworkFrame) e.getParameters().get("frame");
        Channel channel = (Channel) e.getParameters().get("channel");
        ByteBuf bb = Unpooled.buffer();
        NetworkFrame f = new NetworkFrame(frame.toString(), bb);
        String networkName = frame.head("Network-Name");
        if (StringUtil.isEmpty(networkName)) {
            pipeline.nextError(e, new CircuitException("404", String.format("The Network-Name Of Header is Null.")), this);
            return;
        }
        String newnetworkName = frame.head("New-Network-Name");
        if (StringUtil.isEmpty(newnetworkName)) {
            pipeline.nextError(e, new CircuitException("404", String.format("The New-Network-Name Of Header is Null.")), this);
            return;
        }
        if (!container.existsNetwork(networkName)) {
            pipeline.nextError(e, new CircuitException("404", frame.head(String.format("The Network %s is not Exists.", networkName))), this);
            return;
        }
        container.renameNetwork(networkName, newnetworkName);
        NetworkCircuit c = new NetworkCircuit("network/1.0 200 ok");
        c.head("status", "200");
        c.head("message", String.format("The Network %s was renamed.", networkName));
        c.head("Old-Network-Name", networkName);
        c.head("New-Network-Name", newnetworkName);
        bb.writeBytes(c.toByteBuf());
        INetwork network = (INetwork) container.getMasterNetwork();//改道主网络发送,因为系统消息不需要广播
        network.cast(channel, f);
    }

    private void removeNetwork(Event e, IPipeline pipeline) throws CircuitException {
        NetworkFrame frame = (NetworkFrame) e.getParameters().get("frame");
        Channel channel = (Channel) e.getParameters().get("channel");
        ByteBuf bb = Unpooled.buffer();
        NetworkFrame f = new NetworkFrame(frame.toString(), bb);
        String networkName = frame.head("Network-Name");
        if (StringUtil.isEmpty(networkName)) {
            pipeline.nextError(e, new CircuitException("404", String.format("The Network-Name Of Header is Null.")), this);
            return;
        }
        if (!container.existsNetwork(networkName)) {
            pipeline.nextError(e, new CircuitException("404", frame.head(String.format("The Network %s is not Exists.", networkName))), this);
            return;
        }
        container.removeNetwork(networkName);
        NetworkCircuit c = new NetworkCircuit("network/1.0 200 ok");
        c.head("status", "200");
        c.head("message", String.format("The Network %s was removed.", networkName));
        c.head("Network-Name", networkName);
        bb.writeBytes(c.toByteBuf());
        INetwork network = (INetwork) container.getMasterNetwork();//改道主网络发送,因为系统消息不需要广播
        network.cast(channel, f);
    }


    private void existsNetwork(Event e, IPipeline pipeline) throws CircuitException {
        NetworkFrame frame = (NetworkFrame) e.getParameters().get("frame");
        Channel channel = (Channel) e.getParameters().get("channel");
        ByteBuf bb = Unpooled.buffer();
        NetworkFrame f = new NetworkFrame(frame.toString(), bb);
        String networkName = frame.head("Network-Name");
        if (StringUtil.isEmpty(networkName)) {
            pipeline.nextError(e, new CircuitException("404", String.format("The Network-Name Of Header is Null.")), this);
            return;
        }
        NetworkCircuit c = new NetworkCircuit("network/1.0 200 ok");
        if (container.existsNetwork(networkName)) {
            c.head("status", "200");
            c.head("message", "exists");
        } else {
            c.head("status", "404");
            c.head("message", "be not exists");
        }
        c.head("Network-Name", networkName);
        bb.writeBytes(c.toByteBuf());
        INetwork network = (INetwork) container.getMasterNetwork();//改道主网络发送,因为系统消息不需要广播
        network.cast(channel, f);
    }

    private void listNetwork(Event e, IPipeline pipeline) throws CircuitException {
        NetworkFrame frame = (NetworkFrame) e.getParameters().get("frame");
        Channel channel = (Channel) e.getParameters().get("channel");
        String[] names = container.enumNetworkName(true);
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        INetwork manager = container.getMasterNetwork();
        item.put("networkInfo", manager.getInfo());
        item.put("peerNames", manager.enumPeerName());
        list.add(item);
        for (String key : names) {
            INetwork nw = container.getNetwork(key);
            if (nw == null || nw == container.getMasterNetwork()) continue;
            item = new HashMap<>();
            item.put("networkInfo", nw.getInfo());
            item.put("peerNames", nw.enumPeerName());
            list.add(item);
        }
        ByteBuf bb = Unpooled.buffer();
        NetworkFrame f = new NetworkFrame(frame.toString(), bb);
        NetworkCircuit c = new NetworkCircuit("network/1.0 200 ok");
        c.content().writeBytes(new Gson().toJson(list).getBytes());
        bb.writeBytes(c.toByteBuf());
        INetwork network = (INetwork) container.getMasterNetwork();//改道主网络发送,因为系统消息不需要广播
        network.cast(channel, f);
    }

    private void createNetwork(Event e, IPipeline pipeline) throws CircuitException {
        NetworkFrame frame = (NetworkFrame) e.getParameters().get("frame");
        Channel channel = (Channel) e.getParameters().get("channel");
        String name = frame.head("Network-Name");

        if (StringUtil.isEmpty(name)) {
            pipeline.nextError(e, new CircuitException("404", String.format("The Network-Name Of Header is Null.")), this);
            return;
        }
        String castmode = frame.head("Network-Castmode");
        if (StringUtil.isEmpty(castmode)) {
            pipeline.nextError(e, new CircuitException("404", String.format("The Network-Castmode Of Header is Null.")), this);
            return;
        }
        if (container.existsNetwork(name)) {
            pipeline.nextError(e, new CircuitException("500", String.format("The Network %s is Exists.", name)), this);
            return;
        }

        INetwork network = container.createNetwork(name, castmode);
        ByteBuf bb = Unpooled.buffer();
        NetworkFrame succeed = new NetworkFrame(frame.toString(), bb);
        NetworkCircuit c = new NetworkCircuit("network/1.0 200 ok");
        c.head("Network-Name", name);
        c.head("status", "200");
        c.head("message", "The Network be Created.");
        bb.writeBytes(c.toBytes());
        network = (INetwork) container.getMasterNetwork();//改道主网络发送,因为系统消息不需要广播
        network.cast(channel, succeed);//通知成功
    }


}
