package cj.studio.network.nodeapp;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.ultimate.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class Ace {
    String role;
    String right;
    RBACCResource resource;
    List<RBACCResource> except;

    public Ace() {
        except = new ArrayList<>();
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRight() {
        return right;
    }

    public void setRight(String right) {
        this.right = right;
    }

    public RBACCResource getResource() {
        return resource;
    }

    public void setResource(RBACCResource resource) {
        this.resource = resource;
    }


    public void setExcept(List<RBACCResource> except) {
        this.except = except;
    }

    public boolean parse(String aceText) {
        String text = aceText;
        while (text.startsWith(" ")) {
            text = text.substring(1, text.length());
        }
        int pos = text.indexOf(" ");
        if (pos < 0) {
            throw new EcmException("ace格式不正确");
        }
        this.role = text.substring(0, pos);
        text = text.substring(pos + 1, text.length());
        while (text.startsWith(" ")) {
            text = text.substring(1, text.length());
        }
        pos = text.indexOf(" ");
        if (pos < 0) {
            throw new EcmException("ace格式不正确");
        }

        String resText = text.substring(0, pos);
        text = text.substring(pos + 1, text.length());

        this.resource = parseResource(resText);
        ;

        while (text.startsWith(" ")) {
            text = text.substring(1, text.length());
        }
        pos = text.indexOf(" ");
        if (pos < 0) {//没有except
            String right = text;
            if (!"deny".equals(right) && !"allow".equals(right)) {
                CJSystem.logging().warn(getClass(), String.format("Ace:%s 配置权限配置不正确，已被忽略", aceText));
                return false;
            }
            this.right = right;
            return true;
        }

        //有except项
        this.right = text.substring(0, pos);
        text = text.substring(pos + 1, text.length());
        while (text.startsWith(" ")) {
            text = text.substring(1, text.length());
        }
        pos = text.indexOf(" ");
        if (pos < 0) {//虽然有except关键字，但是空的
            return true;
        }
        String except = text.substring(0, pos);
        if (!"except".equals(except)) {
            CJSystem.logging().warn(getClass(), String.format("Ace:%s使用了非except关键字，已被忽略", aceText));
            return false;
        }
        text = text.substring(pos + 1, text.length());
        while (text.startsWith(" ")) {
            text = text.substring(1, text.length());
        }
        while (!StringUtil.isEmpty(text)) {
            pos = text.indexOf(";");
            if (pos < 0) {
                resText = text;
                resource = parseResource(resText);
                this.except.add(resource);
                break;
            }
            resText = text.substring(0, pos);
            resource = parseResource(resText);
            this.except.add(resource);
            text = text.substring(pos + 1, text.length());
            while (text.startsWith(" ")) {
                text = text.substring(1, text.length());
            }
            while (text.startsWith(";")) {
                text = text.substring(1, text.length());
            }
        }
        return true;
    }

    private RBACCResource parseResource(String resText) {
        int pos = resText.indexOf(".");
        if (pos < 0) {
            throw new EcmException("ace格式不正确,网络名与指令之间缺少点号");
        }
        RBACCResource resource = new RBACCResource();
        resource.network = resText.substring(0, pos);
        resource.command = resText.substring(pos + 1, resText.length());
        return resource;
    }


    public boolean hasInExcept(String networkName, boolean isMasterNetwork, String command) {
        for (RBACCResource r : except) {
            boolean namematched = false;
            if (networkName.equals(r.network) || r.network.equals("*") || (r.network.equals("$root") && isMasterNetwork) || (r.network.equals("!root") && !isMasterNetwork)) {
                namematched = true;
            }
            boolean cmdmatched = false;
            if (command.equals(r.command) || r.command.equals("*")) {
                cmdmatched = true;
            }
            if (namematched && cmdmatched) {
                return true;
            }
        }
        return false;
    }
}
