package cj.studio.network.nodeapp.plugin.auth;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.IChip;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceSite;
import cj.studio.network.*;
import cj.studio.util.reactor.IPipeline;

@CjService(name = "$.cj.studio.node.app.plugin", isExoteric = true)
public class NodeAppPlugin implements INodeApplicationAuthPlugin {
    @CjServiceSite
    IServiceProvider site;

    @Override
    public void onstart(String masterNetworkName, IServiceProvider site) {
        IChip chip = (IChip) this.site.getService(IChip.class.getName());
        CJSystem.logging().info(getClass(), String.format("应用插件示例程序:%s 已启动", chip.info().getName()));
    }

    @Override
    public IAuthenticateStrategy createAuthenticateStrategy(String authMode, INetwork network) {
        IChip chip = (IChip) this.site.getService(IChip.class.getName());
        CJSystem.logging().info(getClass(), String.format("%s----createAuthenticateStrategy",chip.info().getName()));
        return null;
    }

    @Override
    public IAccessControllerStrategy createAccessControllerStrategy() {
        IChip chip = (IChip) this.site.getService(IChip.class.getName());
        CJSystem.logging().info(getClass(), String.format("%s----createAccessControllerStrategy",chip.info().getName()));
        return null;
    }
}
