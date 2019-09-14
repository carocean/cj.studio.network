package cj.studio.network.node.server;

import cj.studio.ecm.ServiceCollection;
import cj.studio.util.reactor.DiskStreamEventQueue;
import cj.studio.util.reactor.IServiceProvider;

class ReactorServiceProvider implements cj.studio.util.reactor.IServiceProvider {
    private final IServiceProvider parent;

    public ReactorServiceProvider( IServiceProvider parent) {
        this.parent = parent;
    }

    @Override
    public <T> T getService(String name) {
        return (T) parent.getService(name);
    }

    @Override
    public <T> ServiceCollection<T> getServices(Class<T> clazz) {
        return parent.getServices(clazz);
    }
}