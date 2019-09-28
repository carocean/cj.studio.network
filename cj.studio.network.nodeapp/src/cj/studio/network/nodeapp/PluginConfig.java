package cj.studio.network.nodeapp;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginConfig implements IPluginConfig {
    boolean disableAuth;
    List<String> disableOthers;
    @Override
    public boolean isDisableAuth() {
        return disableAuth;
    }
    @Override
    public boolean containsDisableOthers(String pluginName) {
        return disableOthers.contains(pluginName);
    }

    @Override
    public void load(String home) throws FileNotFoundException {
        String fn = String.format("%s%sconf/plugin.yaml", home, File.separator);
        File file = new File(fn);
        if (!file.exists()) {
            throw new EcmException(String.format("不存在配置文件：%s", fn));
        }
        Reader reader = new FileReader(file);
        Yaml yaml = new Yaml();
        Map<String, Object> config = yaml.load(reader);
        Object disableAuth = config.get("disable-auth");
        if (disableAuth == null) {
            this.disableAuth = false;
        } else {
            this.disableAuth = (boolean) disableAuth;
            CJSystem.logging().info(getClass(), String.format("Auth插件配置为：%s", (this.disableAuth?"不可用":"可用")));
        }
        disableOthers = new ArrayList<>();
        List<Object> others = (List<Object>) config.get("disable-others");
        if (others == null) return;
        for (Object obj : others) {
            disableOthers.add((String) obj);
            CJSystem.logging().info(getClass(), String.format("插件：%s 已配置为失活", obj));
        }
    }
}
