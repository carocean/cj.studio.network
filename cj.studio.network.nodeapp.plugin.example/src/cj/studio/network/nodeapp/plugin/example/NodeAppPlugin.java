package cj.studio.network.nodeapp.plugin.example;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.IChip;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceSite;
import cj.studio.network.INetwork;
import cj.studio.network.INodeApplicationPlugin;
import cj.studio.network.UserPrincipal;
import cj.studio.util.reactor.IPipeline;

@CjService(name = "$.cj.studio.node.app.plugin", isExoteric = true)
public class NodeAppPlugin implements INodeApplicationPlugin {
    @CjServiceSite
    IServiceProvider site;

    @Override
    public void oninactiveNetwork(INetwork network, IPipeline pipeline) {
        IChip chip = (IChip)this. site.getService(IChip.class.getName());
        CJSystem.logging().info(getClass(), String.format("%s----oninactiveNetwork",chip.info().getName()));
    }

    @Override
    public void onactivedNetwork(UserPrincipal userPrincipal, INetwork network, IPipeline pipeline) {
        IChip chip = (IChip) this.site.getService(IChip.class.getName());
        CJSystem.logging().info(getClass(), String.format("%s----onactivedNetwork",chip.info().getName()));
    }

    @Override
    public void onstart(String masterNetworkName, IServiceProvider site) {
        IChip chip = (IChip) this.site.getService(IChip.class.getName());
        CJSystem.logging().info(getClass(), String.format("应用插件示例程序:%s 已启动", chip.info().getName()));
    }
}
