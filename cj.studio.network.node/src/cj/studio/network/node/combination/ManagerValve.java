package cj.studio.network.node.combination;

import cj.studio.ecm.net.CircuitException;
import cj.studio.network.Circuit;
import cj.studio.network.Frame;
import cj.studio.network.INetwork;
import cj.studio.network.NetworkInfo;
import cj.studio.network.node.INetworkContainer;
import cj.studio.util.reactor.Event;
import cj.studio.util.reactor.IPipeline;
import cj.studio.util.reactor.IValve;
import cj.ultimate.gson2.com.google.gson.Gson;
import cj.ultimate.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.List;

/**
 * 处理管理网络逻辑
 */
public class ManagerValve implements IValve {
    String managerNetworkName;

    public ManagerValve(String name) {
        managerNetworkName = name;
    }

    @Override
    public void nextError(Event e, Throwable error, IPipeline pipeline) throws CircuitException {
        pipeline.nextError(e, error, this);
    }

    @Override
    public void flow(Event e, IPipeline pipeline) throws CircuitException {
        if (!managerNetworkName.equals(pipeline.key())) {//不是管理网络则放过去
            pipeline.nextFlow(e, this);
            return;
        }
        INetworkContainer container = (INetworkContainer) pipeline.site().getService("$.network.container");
        switch (e.getCmd()) {
            case "error":
                errorNetwork(e, pipeline, container);
                break;
            case "infoNetwork":
                infoNetwork(e, pipeline, container);
                break;
            case "createNetwork":
                createNetwork(e, pipeline, container);
                break;
            case "listNetwork":
                listNetwork(e, pipeline, container);
                break;
            case "existsNetwork":
                existsNetwork(e, pipeline, container);
                break;
            case "removeNetwork":
                removeNetwork(e, pipeline, container);
                break;
            case "renameNetwork":
                renameNetwork(e, pipeline, container);
                break;
            case "changeCastmode":
                changeCastmode(e, pipeline, container);
                break;
            default:
                pipeline.nextError(e, new CircuitException("501", String.format("The Command %s is not Surported", e.getCmd())), this);
                break;
        }
    }

    private void infoNetwork(Event e, IPipeline pipeline, INetworkContainer container) throws CircuitException {
        Frame frame = (Frame) e.getParameters().get("frame");
        Channel channel = (Channel) e.getParameters().get("channel");
        ByteBuf bb = Unpooled.buffer();
        Frame f = new Frame(frame.toString(), bb);
        String networkName = frame.head("Network-Name");
        if (StringUtil.isEmpty(networkName)) {
            pipeline.nextError(e, new CircuitException("404", String.format("The Network-Name Of Header is Null.")), this);
            return;
        }

        Circuit c = new Circuit("network/1.0 200 ok");
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
        e.getParameters().put("frame", f);
        pipeline.nextFlow(e, this);
    }

    private void changeCastmode(Event e, IPipeline pipeline, INetworkContainer container) throws CircuitException {
        Frame frame = (Frame) e.getParameters().get("frame");
        Channel channel = (Channel) e.getParameters().get("channel");
        ByteBuf bb = Unpooled.buffer();
        Frame f = new Frame(frame.toString(), bb);
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
        Circuit c = new Circuit("network/1.0 200 ok");
        c.head("status", "200");
        c.head("message", String.format("The Castmode Of Network %s was change.", networkName));
        c.head("Network-Name", networkName);
        c.head("Old-Network-Castmode", container.getNetwork(networkName).getInfo().getCastmode());
        c.head("New-Network-Castmode", networkName);
        bb.writeBytes(c.toByteBuf());

        e.getParameters().put("frame", f);
        pipeline.nextFlow(e, this);
    }

    private void renameNetwork(Event e, IPipeline pipeline, INetworkContainer container) throws CircuitException {
        Frame frame = (Frame) e.getParameters().get("frame");
        Channel channel = (Channel) e.getParameters().get("channel");
        ByteBuf bb = Unpooled.buffer();
        Frame f = new Frame(frame.toString(), bb);
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
        Circuit c = new Circuit("network/1.0 200 ok");
        c.head("status", "200");
        c.head("message", String.format("The Network %s was renamed.", networkName));
        c.head("Old-Network-Name", networkName);
        c.head("New-Network-Name", networkName);
        bb.writeBytes(c.toByteBuf());
        e.getParameters().put("frame", f);
        pipeline.nextFlow(e, this);
    }

    private void removeNetwork(Event e, IPipeline pipeline, INetworkContainer container) throws CircuitException {
        Frame frame = (Frame) e.getParameters().get("frame");
        Channel channel = (Channel) e.getParameters().get("channel");
        ByteBuf bb = Unpooled.buffer();
        Frame f = new Frame(frame.toString(), bb);
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
        Circuit c = new Circuit("network/1.0 200 ok");
        c.head("status", "200");
        c.head("message", String.format("The Network %s was removed.", networkName));
        c.head("Network-Name", networkName);
        bb.writeBytes(c.toByteBuf());
        e.getParameters().put("frame", f);
        pipeline.nextFlow(e, this);
    }

    private void errorNetwork(Event e, IPipeline pipeline, INetworkContainer container) throws CircuitException {
        Frame frame = (Frame) e.getParameters().get("frame");
        pipeline.nextError(e, new CircuitException(frame.head("status"), frame.head("message")), this);
    }

    private void existsNetwork(Event e, IPipeline pipeline, INetworkContainer container) throws CircuitException {
        Frame frame = (Frame) e.getParameters().get("frame");
        Channel channel = (Channel) e.getParameters().get("channel");
        ByteBuf bb = Unpooled.buffer();
        Frame f = new Frame(frame.toString(), bb);
        String networkName = frame.head("Network-Name");
        if (StringUtil.isEmpty(networkName)) {
            pipeline.nextError(e, new CircuitException("404", String.format("The Network-Name Of Header is Null.")), this);
            return;
        }
        Circuit c = new Circuit("network/1.0 200 ok");
        if (container.existsNetwork(networkName)) {
            c.head("status", "200");
            c.head("message", "exists");
        } else {
            c.head("status", "404");
            c.head("message", "be not exists");
        }
        c.head("Network-Name", networkName);
        bb.writeBytes(c.toByteBuf());
        e.getParameters().put("frame", f);
        pipeline.nextFlow(e, this);
    }

    private void listNetwork(Event e, IPipeline pipeline, INetworkContainer container) throws CircuitException {
        Frame frame = (Frame) e.getParameters().get("frame");
        Channel channel = (Channel) e.getParameters().get("channel");
        String[] names = container.enumNetworkName(true);
        List<NetworkInfo> list = new ArrayList<>();
        list.add(container.getManagerNetworkInfo());
        for (String key : names) {
            INetwork nw = container.getNetwork(key);
            if (nw == null || nw == container.getManagerNetwork()) continue;
            list.add(nw.getInfo());
        }
        ByteBuf bb = Unpooled.buffer();
        Frame f = new Frame(frame.toString(), bb);
        Circuit c = new Circuit("network/1.0 200 ok");
        c.content().writeBytes(new Gson().toJson(list).getBytes());
        bb.writeBytes(c.toByteBuf());
        e.getParameters().put("frame", f);
        pipeline.nextFlow(e, this);
    }

    private void createNetwork(Event e, IPipeline pipeline, INetworkContainer container) throws CircuitException {
        Frame frame = (Frame) e.getParameters().get("frame");
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
        network.addChannel(channel);
        ByteBuf bb = Unpooled.buffer();
        Frame succeed = new Frame(frame.toString(), bb);
        Circuit c = new Circuit("network/1.0 200 ok");
        c.head("Network-Name", name);
        c.head("status", "200");
        c.head("message", "The Network be Created.");
        bb.writeBytes(c.toBytes());
        e.getParameters().put("frame", succeed);
        pipeline.nextFlow(e, this);//通知成功
    }


}
