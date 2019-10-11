package cj.studio.network.nodeapp.plugin.auth;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.IChip;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceSite;
import cj.studio.network.*;
import cj.studio.util.reactor.IPipeline;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

@CjService(name = "$.cj.studio.node.app.plugin", isExoteric = true)
public class NodeAppPlugin implements INodeApplicationAuthPlugin {
    @CjServiceSite
    IServiceProvider site;
    OkHttpClient okHttpClient;
    private String appid;

    @Override
    public void onstart(String masterNetworkName, IServiceProvider site) {
        IChip chip = (IChip) this.site.getService(IChip.class.getName());
        this.appid = chip.site().getProperty("uc.appid");
        String maxIdleConnections = chip.site().getProperty("okhttp.pool.maxIdleConnections");
        String keepAliveDuration = chip.site().getProperty("okhttp.pool.keepAliveDuration");
        String readTimeout = chip.site().getProperty("okhttp.readTimeout");
        String connectTimeout = chip.site().getProperty("okhttp.connectTimeout");
        String writeTimeout = chip.site().getProperty("okhttp.writeTimeout");
        String callTimeout = chip.site().getProperty("okhttp.callTimeout");
        ConnectionPool connectionPool = new ConnectionPool(Integer.valueOf(maxIdleConnections), Long.valueOf(keepAliveDuration), TimeUnit.SECONDS);
        okHttpClient = new OkHttpClient()
                .newBuilder()
                .connectionPool(connectionPool)
                .readTimeout(Integer.valueOf(readTimeout), TimeUnit.SECONDS)
                .connectTimeout(Integer.valueOf(connectTimeout), TimeUnit.SECONDS)
                .writeTimeout(Integer.valueOf(writeTimeout), TimeUnit.SECONDS)
                .callTimeout(Integer.valueOf(callTimeout), TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
        CJSystem.logging().info(getClass(), String.format("应用插件示例程序:%s 已启动", chip.info().getName()));
    }

    @Override
    public IAuthenticateStrategy createAuthenticateStrategy(String authMode, INetwork network) {
        return new UcAuthenticateStrategy(okHttpClient, authMode, network, appid);
    }


    @Override
    public IAccessControllerStrategy createAccessControllerStrategy() {
        IChip chip = (IChip) this.site.getService(IChip.class.getName());
        CJSystem.logging().info(getClass(), String.format("%s----createAccessControllerStrategy", chip.info().getName()));
//        return new UcAccessControllerStrategy(site);
        return null;//返回空表示使用rbac.yaml中的acl配置
    }
}
