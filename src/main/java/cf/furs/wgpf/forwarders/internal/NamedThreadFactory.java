package cf.furs.wgpf.forwarders.internal;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {
    private final String prefix;
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    public NamedThreadFactory(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setName(prefix + threadNumber.getAndIncrement());
        return thread;
    }
}