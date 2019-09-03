package cj.studio.network.peer;

import cj.studio.ecm.IServiceProvider;
import cj.studio.network.Frame;

public interface IOnmessage {
    void onmessage(Frame frame, IServiceProvider site);
}
