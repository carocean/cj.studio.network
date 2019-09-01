package cj.studio.network.node.combination;

import cj.studio.ecm.net.CircuitException;
import cj.studio.util.reactor.Event;
import cj.studio.util.reactor.IPipeline;
import cj.studio.util.reactor.IPipelineCombination;
import cj.studio.util.reactor.IValve;

public class ReactorPipelineCombination implements IPipelineCombination {
    @Override
    public void combine(IPipeline pipeline) {
        pipeline.append(new IValve() {
                @Override
                public void flow(Event e, IPipeline pipeline) throws CircuitException {
                        System.out.println(e.getKey()+" "+e.getCmd());
                }

                @Override
                public void nextError(Event e, Throwable error, IPipeline pipeline) throws CircuitException {
                        System.out.println(e.getKey()+" "+e.getCmd()+" "+error);
                }
        });
    }

    @Override
    public void demolish(IPipeline pipeline) {

    }
}