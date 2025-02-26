package cf.furs.wgpf.forwarders;

import cf.furs.wgpf.forwarders.internal.DSWrapper;
import cf.furs.wgpf.internal.CoCoder;
import cf.furs.wgpf.internal.DeferredWGRPAnalyze;
import cf.furs.wgpf.internal.MagicFunction;

import java.net.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.*;

public class ForwarderUDP extends AbstractForwarder {
    public static final int UDP_FRAME_BUFFER_SIZE = 64 * 1024;
    private DatagramSocket serverDS;
    private final ConcurrentHashMap<Integer, DSWrapper> dsBinds = new ConcurrentHashMap<>();
    private final static long WATCHDOG_ITER_DELAY_MS = 15 * 1_000;

    // private final static int BUFFER_POOL_SIZE = 1024;
    private final static int BUFFER_POOL_SIZE = 4;

    public static DeferredWGRPAnalyze wgrpAnalyzer = null;

    // private final int CORE_POOL_SIZE = 64; // min threads
    private final int CORE_POOL_SIZE = BUFFER_POOL_SIZE; // min threads
    // private final int MAXIMUM_POOL_SIZE = Integer.MAX_VALUE;
    private final int MAXIMUM_POOL_SIZE = BUFFER_POOL_SIZE;
    private final long KEEP_ALIVE_TIME = 60L; // threads ttl (s)

    private int clientShift;
    private final int serverShift;

    private final MagicFunction magicFunction;

    public ForwarderUDP(Integer listPort, String destHost, Integer destPort, int magicType, int clientShift, int serverShift) throws UnknownHostException {
        super(listPort, destHost, destPort, magicType);
        this.magicFunction = CoCoder.getMagicFunc(this.getForwarderType());
        this.clientShift = clientShift;
        this.serverShift = serverShift;
    }
    @Override
    public void startThread() {
        if(this.magicFunction==null)
            this.clientShift=0;
        new Thread(()->{
            try {
                System.out.println("Init buffers..");
                ArrayBlockingQueue<byte[]> bufferPool = new ArrayBlockingQueue<>(BUFFER_POOL_SIZE);
                for (int i = 0; i < BUFFER_POOL_SIZE; i++) {
                    bufferPool.add(new byte[UDP_FRAME_BUFFER_SIZE]); // Заполняем пул буферами
                }
                this.serverDS = new DatagramSocket(this.getListPort());
                setActive(true);
                System.out.println("UDP server is running on port "+getListPort()+" for "+this.getResolvedAddress()+":"+this.getDestPort());


                if(wgrpAnalyzer == null) {
                    wgrpAnalyzer = new DeferredWGRPAnalyze();
                }
                wgrpAnalyzer.startAnalyze();
                Runtime.getRuntime().addShutdownHook(new Thread(() -> wgrpAnalyzer.stopAnalyze()));

                new Thread(()->{
                    try {
                        while(this.serverIsActive()) {
                            Thread.sleep(WATCHDOG_ITER_DELAY_MS);
                            dsBinds.forEach(this::idleInspect);
                        }
                    } catch (Exception e) {
                        // System.err.println("WatchDog Common err");
                    }
                },"DS-WATCHDOG").start();

                ExecutorService executor = new ThreadPoolExecutor(
                        CORE_POOL_SIZE,
                        MAXIMUM_POOL_SIZE,
                        KEEP_ALIVE_TIME,
                        TimeUnit.SECONDS,
                        new LinkedBlockingQueue<Runnable>()
                );
                // ExecutorService executor = Executors.newCachedThreadPool();

                while (serverIsActive()) {
                    byte[] packetBuffer = bufferPool.take();
                    DatagramPacket proxiedDP = new DatagramPacket(packetBuffer, packetBuffer.length);
                    serverDS.receive(proxiedDP); // Принимаем пакет
                    // System.out.println("Новый пакет от "+proxiedDP.getAddress()+":"+proxiedDP.getPort());
                    executor.submit(() -> { // Передаем задачу в пул потоков
                        try {
                            DSWrapper dsWrapper = getDestDS(proxiedDP, this.getDestHost(), this.getDestPort(), this.magicFunction, this.serverShift);
                            if (this.clientShift != 0) {
                                this.magicFunction.magic(proxiedDP.getData(), proxiedDP.getLength(), this.clientShift);
                            }
                            // proxiedDP.setData(buffer,0,proxiedDP.getLength());
                            proxiedDP.setAddress(getResolvedAddress());
                            proxiedDP.setPort(getDestPort());
                            dsWrapper.getDs().send(proxiedDP);
                            //System.out.println("Источник -> Отправлен DDS'у");
                        } catch (Exception e) {
                            e.printStackTrace(); // Обрабатываем исключения
                        } finally {
                            bufferPool.offer(packetBuffer); // Возвращаем буфер в пул после использования
                        }
                    });
                }
            } catch (Exception e) {
                setActive(false);
                System.err.println("Common DS-ACCEPTOR In err: " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        },"DS-ACCEPTOR").start();
    }

    //synchronized private DSWrapper getDestDS(SocketAddress sa, InetAddress originAddress, int originPort) throws SocketException {
    synchronized private DSWrapper getDestDS(DatagramPacket dp, String destHost, int destPort, MagicFunction magicFunction, int serverShift) throws SocketException {
        Integer hash = calculateHash(dp.getAddress(),dp.getPort());

        DSWrapper dds = dsBinds.get(hash);
        if(dds == null) {
            // System.out.println("DDS создан "+dp.getAddress()+":"+dp.getPort());
            dds = new DSWrapper(new DatagramSocket(), serverDS, dp.getAddress(), dp.getPort());
            dds.setMagicFunction(magicFunction);
            dds.setServerShift(serverShift);
            dds.setDestHost(destHost);
            dds.setDestPort(destPort);
            dsBinds.put(hash,dds);
        }
        dds.makeActive();
        //System.out.println("DDS найден "+dds.getDs().getLocalAddress()+":"+dds.getDs().getLocalPort());
        return dds;
    }
    private int calculateHash(InetAddress address, int port) {
        // int hash = Objects.hash(Arrays.hashCode(address.getAddress()), port);
        // System.out.println("HASH FOR "+address+":"+port+" - "+hash);
        return Objects.hash(Arrays.hashCode(address.getAddress()), port);
    }

    private void idleInspect(Integer hashKey, DSWrapper dsWrapper) {
        try {
            if (dsWrapper.isIdle()) {
                //System.out.println("DS IS IDLE: \n\t\t" + dsWrapper.getDs().getLocalAddress());
                dsWrapper.destroy();
                dsBinds.remove(hashKey);
            }
        } catch (Exception e) {
            // System.err.println("WatchDog Iter err");
        }
    }
}
