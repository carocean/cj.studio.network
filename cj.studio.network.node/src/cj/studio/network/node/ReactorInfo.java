package cj.studio.network.node;

import java.util.Map;

public class ReactorInfo {
    int workThreadCount;
    int queueCapacity;
    public ReactorInfo(int workThreadCount, int queueCapacity) {
        this.workThreadCount = workThreadCount;
        this.queueCapacity = queueCapacity;
    }

    public static ReactorInfo parse(Map<String, Object> node) {
        Map<String, Object> reactor = (Map<String, Object>) node.get("reactor");
        int workThreadCount = reactor.get("workThreadCount")==null?8:(int)reactor.get("workThreadCount");
        int queueCapacity = reactor.get("queueCapacity")==null?1000:(int)reactor.get("queueCapacity");
        ReactorInfo reactorInfo = new ReactorInfo(workThreadCount, queueCapacity);
        return reactorInfo;
    }

    public int workThreadCount() {
        return workThreadCount;
    }


    public int queueCapacity() {
        return queueCapacity;
    }

}
