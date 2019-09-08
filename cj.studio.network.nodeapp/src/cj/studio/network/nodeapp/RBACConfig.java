package cj.studio.network.nodeapp;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.network.nodeapp.strategy.UserInfo;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RBACConfig implements IRBACConfig {
    private final String masterNetworkName;
    private boolean isEnableRBAC;
    private List<String> roles;
    Map<String, UserInfo> users;
    private Acl acl;

    public RBACConfig(String masterNetworkName) {
        this.masterNetworkName=masterNetworkName;
    }

    @Override
    public void load(String home) throws FileNotFoundException {
        String fn = String.format("%s%sconf/rbac.yaml", home, File.separator);
        File file = new File(fn);
        if (!file.exists()) {
            throw new EcmException(String.format("不存在配置文件：%s", fn));
        }
        Reader reader = new FileReader(file);
        Yaml yaml = new Yaml();
        Map<String, Object> rbac = yaml.load(reader);

        Object enableObj = rbac.get("enable");
        this.isEnableRBAC = enableObj == null ? false : (boolean) enableObj;
        this.roles = (List<String>) rbac.get("roles");
        if (this.roles == null) {
            this.roles = new ArrayList<>();
        }
        List<Map<String, Object>> users = (List<Map<String, Object>>) rbac.get("users");
        if (users != null) {
            parseUsers(users);
        }
        List<String> acl = (List<String>) rbac.get("acl");
        if (acl != null) {
            parseAcl(acl);
        }
    }

    private void parseAcl(List<String> acl) {
        this.acl=new Acl();
        for (String aceText : acl) {
            Ace ace = new Ace();
            if(!ace.parse(aceText)){
                continue;
            }
            this.acl.add(ace);
        }
    }

    private void parseUsers(List<Map<String, Object>> users) {
        this.users = new HashMap<>();
        for (Map<String, Object> u : users) {
            List<String> uroles = new ArrayList<>();
            List<String> inRoles = (List<String>) u.get("roles");
            if (inRoles != null) {
                for (String r : inRoles) {
                    if (!this.roles.contains(r)) {
                        CJSystem.logging().info(getClass(), String.format("用户的角色成员：%s 在角色列表中未被定义,已忽略", r));
                        continue;
                    }
                    uroles.add(r);
                }
            }
            UserInfo info = new UserInfo(u.get("name") + "", u.get("pwd") + "", uroles);
            this.users.put(info.getName(), info);
        }
    }
    @Override
    public Acl getAcl() {
        return acl;
    }

    @Override
    public boolean isEnableRBAC() {
        return isEnableRBAC;
    }

    @Override
    public UserInfo getUserInfo(String authUser) {
        return users.get(authUser);
    }
    @Override
    public String getMasterNetworkName() {
        return masterNetworkName;
    }
}
