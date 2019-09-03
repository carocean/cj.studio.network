package cj.studio.network.peer;

import cj.studio.ecm.IServiceProvider;
import cj.studio.network.NetworkFrame;

public interface IOnmessage {
    void onmessage(NetworkFrame frame, IServiceProvider site);
}
