package cj.studio.network.nodeapp.plugin.auth;

import cj.studio.ecm.IServiceProvider;
import cj.studio.network.AuthenticationException;
import cj.studio.network.IAuthenticateStrategy;
import cj.studio.network.INetwork;
import cj.studio.network.UserPrincipal;
import cj.ultimate.gson2.com.google.gson.Gson;
import cj.ultimate.gson2.com.google.gson.reflect.TypeToken;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 与用户中心对接
 */
public class UcAuthenticateStrategy implements IAuthenticateStrategy {
    String authModel;
    INetwork network;
    IServiceProvider site;
    OkHttpClient okHttpClient;
    public UcAuthenticateStrategy(OkHttpClient okHttpClient ,String authMode, INetwork network, IServiceProvider site) {
        this.authModel = authMode;
        this.network = network;
        this.site = site;
        this.okHttpClient=okHttpClient;
    }

    //用okhttp访问用户中心
    @Override
    public UserPrincipal authenticate(String authUser, String authToken) throws AuthenticationException {
        String url = String.format("http://47.105.165.186/uc/auth?appid=gbera&accountName=%s&password=%s", authUser, authToken);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Rest-Command", "auth")
                .get()
                .build();
        Call call = okHttpClient.newCall(request);
        Response response = null;
        UserPrincipal userPrincipal = null;
        try {
            response = call.execute();
            byte[] data = response.body().bytes();
            String json = new String(data);
            Map<String, Object> rc = new Gson().fromJson(json, new TypeToken<HashMap<String, Object>>() {
            }.getType());
            String text = (String) rc.get("dataText");
            Double status = (Double) rc.get("status");
            if (status >= 400) {
                throw new AuthenticationException(String.format("远程错误。原因：%s %s", status, rc.get("message")));
            }
            Map<String, Object> result = new Gson().fromJson(text, new TypeToken<HashMap<String, Object>>() {
            }.getType());
            userPrincipal = new UserPrincipal((String) result.get("uid"));
            List<Map<String, Object>> roles = (List<Map<String, Object>>) result.get("appRoles");
            for (Map<String, Object> objRole : roles) {
                String appid=(String) objRole.get("appId");
                if(!"gbera".equals(appid)){
                    continue;
                }
                String roleid=(String) objRole.get("roleId");
                userPrincipal.addRole(roleid);
            }
            return userPrincipal;
        } catch (IOException e) {
            throw new AuthenticationException(e);
        }


    }
}
