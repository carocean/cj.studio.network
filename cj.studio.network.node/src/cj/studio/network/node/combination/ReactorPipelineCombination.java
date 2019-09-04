package cj.studio.network.node.combination;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;
import cj.studio.network.NetworkFrame;
import cj.studio.network.node.INetwork;
import cj.studio.network.node.INetworkContainer;
import cj.studio.util.reactor.Event;
import cj.studio.util.reactor.IPipeline;
import cj.studio.util.reactor.IPipelineCombination;
import io.netty.channel.Channel;



public class ReactorPipelineCombination implements IPipelineCombination {
    INetworkContainer container;
    public ReactorPipelineCombination(IServiceProvider parent) {
        container=(INetworkContainer)parent.getService("$.network.container");
    }

    @Override
    public void combine(IPipeline pipeline) {
        //此处增加规则是：一个pipeline对应一个network，因为reactor的key就是network
        Event event=(Event) pipeline.attachment();
        Channel ch=(Channel) event.getParameters().get("channel");//取出channel
        INetwork network=container.getNetwork(pipeline.key());
        if(network==null){
           throw new EcmException("不存在网络："+pipeline.key());
        }
        network.addChannel(ch);//将channel添加到network，这样network便有了输出能力
        pipeline.attachment(network);//将网络附到管道上供app中的valve使用
        pipeline.append(new ManagerValve(container.getManagerNetworkInfo().getName()));//管理网络的处理放在最上面
        pipeline.append(new CastValve());
    }



    @Override
    public void demolish(IPipeline pipeline) {
        container.removeNetwork(pipeline.key());
        pipeline.attachment(null);
    }
}