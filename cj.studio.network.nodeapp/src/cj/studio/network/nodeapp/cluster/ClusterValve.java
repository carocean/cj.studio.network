package cj.studio.network.nodeapp.cluster;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.net.CircuitException;
import cj.studio.util.reactor.Event;
import cj.studio.util.reactor.IPipeline;
import cj.studio.util.reactor.IValve;

public class ClusterValve implements IValve {
    @Override
    public void flow(Event e, IPipeline pipeline) throws CircuitException {
        CJSystem.logging().info(getClass(),"-------ClusterValve.flow");
        pipeline.nextFlow(e,this);
    }

    @Override
    public void nextError(Event e, Throwable error, IPipeline pipeline) throws CircuitException {
        CJSystem.logging().info(getClass(),"-------ClusterValve.nextError");
        pipeline.nextError(e,error,this);
    }
}
