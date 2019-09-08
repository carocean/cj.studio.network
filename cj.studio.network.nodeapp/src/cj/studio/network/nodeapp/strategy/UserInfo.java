package cj.studio.network.nodeapp.strategy;

import java.util.List;

public class UserInfo {
    String name;
    String pwd;
    List<String> roles;

    public UserInfo() {
    }

    public UserInfo(String name, String pwd, List<String> roles) {
        this.name = name;
        this.pwd = pwd;
        this.roles = roles;
    }

    public String getPwd() {
        return pwd;
    }

    public String getName() {
        return name;
    }

    public List<String> getRoles() {
        return roles;
    }
}
