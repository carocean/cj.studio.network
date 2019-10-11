package cj.studio.network;

import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户主体信息
 */
public final class UserPrincipal {
    String name;//用户名
    List<String> roles;

    public UserPrincipal(String name) {
        this.name = name;
        this.roles = new ArrayList<>();
    }

    /**
     * 从通道中获取当前已认证的用户
     * @param channel
     * @return
     */
    public static UserPrincipal get(Channel channel){
        Attribute<UserPrincipal> userPrincipalAttribute=channel.attr(AttributeKey.valueOf("Peer-UserPrincipal"));
        if(userPrincipalAttribute==null){
            return null;
        }
        return userPrincipalAttribute.get();
    }
    public String toRoles(){
        String text="";
        for(String r:roles){
            text=text+r+";";
        }
        while (text.endsWith(";")){
            text=text.substring(0,text.length()-1);
        }
        return text;
    }
    public void addRole(String role){
        roles.add(role);
    }
    /**
     * 是否包括角色
     * @param role
     * @return
     */
    public boolean hasRole(String role){
       return roles.contains(role);
    }

    /**
     * 用户名
     * @return
     */
    public String getName() {
        return name;
    }
}
