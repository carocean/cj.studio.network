package cj.studio.network.node.combination;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.net.CircuitException;
import cj.studio.network.NetworkCircuit;
import cj.studio.network.NetworkFrame;
import cj.studio.network.node.INetwork;
import cj.studio.network.node.INetworkContainer;
import cj.studio.util.reactor.*;
import cj.ultimate.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;


public class ReactorPipelineCombination implements IPipelineCombination {
    INetworkContainer container;

    public ReactorPipelineCombination(IServiceProvider parent) {
        container = (INetworkContainer) parent.getService("$.network.container");
    }

    @Override
    public void combine(IPipeline pipeline) throws CombineException {
        //此处增加规则是：一个pipeline对应一个network，因为reactor的key就是network
        Event event = (Event) pipeline.attachment();
        INetwork network = container.getNetwork(pipeline.key());
        if (network == null) {//如果没有该网络则检查创建策略
            if (!container.isAutoCreateNetwork()) {
                notExistsNetworkError(event, pipeline.key(), pipeline.site());
                throw new CombineException(String.format("未建立管道：%s", pipeline.key()));
            }
            //下面是自动创建，但要检查是否有网络名和传播类型的参数，如果没有报异常
            network = autoCreateNetwork(event, pipeline);
        }
        Channel ch = (Channel) event.getParameters().get("channel");//取出channel
        network.addChannel(ch);//将channel添加到network，这样network便有了输出能力
        pipeline.attachment(network);//将网络附到管道上供app中的valve使用
        pipeline.append(new CheckAutoCreateNetworkValve(container));
        pipeline.append(new ManagerValve(container));//管理网络的处理放在最上面
        pipeline.append(new CastValve());
    }

    private INetwork autoCreateNetwork(Event event, IPipeline pipeline) {
        NetworkFrame frame = (NetworkFrame) event.getParameters().get("frame");
        String networkName = frame.rootName();
        String networkCastmode = frame.head("Network-Castmode");
        if (StringUtil.isEmpty(networkCastmode)) {
            networkCastmode = "multicast";//默认为多播
        }
        return container.createNetwork(networkName, networkCastmode);
    }

    //因为此时新的pipeline还未添加Valve，只能找到主管道向后发送错误
    private void notExistsNetworkError(Event e, String notExistsNeworkName, IServiceProvider site) {
        //发给管理命令
        ISelectionKey key = (ISelectionKey) site.getService(String.format("$.key.%s", container.getMasterNetworkName()));
        IPipeline masterPipeline = key.pipeline();
        try {
            masterPipeline.error(e, new CircuitException("404", String.format("The Network %s is Not Exists.", notExistsNeworkName)));
        } catch (CircuitException ex) {
            CJSystem.logging().error(getClass(), e);
            return;
        }
    }


    @Override
    public void demolish(IPipeline pipeline) {
        container.removeNetwork(pipeline.key());
        pipeline.attachment(null);
    }
}