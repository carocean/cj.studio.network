package cj.studio.network.nodeapp;

import java.io.FileNotFoundException;

public interface IPluginConfig {
    boolean isDisableAuth();

    boolean containsDisableOthers(String pluginName);

    void load(String home) throws FileNotFoundException;

}
