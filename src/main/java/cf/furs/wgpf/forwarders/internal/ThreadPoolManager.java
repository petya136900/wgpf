package cf.furs.wgpf.forwarders.internal;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPoolManager {
    private final ExecutorService accepterPool;
    private final ExecutorService readerPool;
    private final ExecutorService writerPool;

    public ThreadPoolManager() {
        accepterPool = Executors.newCachedThreadPool(new NamedThreadFactory("ACCEPTER-"));
        readerPool = Executors.newCachedThreadPool(new NamedThreadFactory("READER-"));
        writerPool = Executors.newCachedThreadPool(new NamedThreadFactory("WRITER-"));
    }

    public ExecutorService getAccepterPool() {
        return accepterPool;
    }

    public ExecutorService getReaderPool() {
        return readerPool;
    }

    public ExecutorService getWriterPool() {
        return writerPool;
    }
}