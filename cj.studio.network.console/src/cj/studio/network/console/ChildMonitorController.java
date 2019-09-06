package cj.studio.network.console;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ChildMonitorController {
    ReentrantLock childMonitorLock = new ReentrantLock();
    Condition childMonitorCond = childMonitorLock.newCondition();
    AtomicBoolean notEntryChildMonitor=new AtomicBoolean(false);;
    public void singleAll(boolean notEntryChildMonitor){
        this.notEntryChildMonitor.set(notEntryChildMonitor);
        try {
            childMonitorLock.lock();
            childMonitorCond.signalAll();
        } finally {
            childMonitorLock.unlock();
        }
    }

    public boolean isNotEntryChildMonitor() {
        return notEntryChildMonitor.get();
    }

    public void await() throws InterruptedException {
        try {
            childMonitorLock.lock();
            childMonitorCond.await();
        } finally {
            childMonitorLock.unlock();
        }
    }
}
