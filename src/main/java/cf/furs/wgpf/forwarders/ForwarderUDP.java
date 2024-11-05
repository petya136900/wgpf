package cf.furs.wgpf.forwarders;

import cf.furs.wgpf.forwarders.internal.DSWrapper;
import cf.furs.wgpf.internal.DeferredWGRPAnalyze;

import java.net.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ForwarderUDP extends AbstractForwarder {
    public static final int UDP_FRAME_BUFFER_SIZE = 64 * 1024;
    private DatagramSocket serverDS;
    private final ConcurrentHashMap<Integer, DSWrapper> dsBinds = new ConcurrentHashMap<>();
    private final static long WATCHDOG_ITER_DELAY_MS = 1_000;

    public static DeferredWGRPAnalyze wgrpAnalyzer = null;

    public ForwarderUDP(Integer listPort, String destHost, Integer destPort, int forwarderType) throws UnknownHostException {
        super(listPort, destHost, destPort, forwarderType);
    }
    @Override
    public void startThread() {
        new Thread(()->{
            try {
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

                byte[] buffer = new byte[UDP_FRAME_BUFFER_SIZE];
                while(serverIsActive()) {
                    DatagramPacket proxiedDP  = new DatagramPacket(buffer, buffer.length);
                    serverDS.receive(proxiedDP);
                    // System.out.println("Новый пакет от "+proxiedDP.getAddress()+":"+proxiedDP.getPort());
                    DSWrapper dsWrapper = getDestDS(proxiedDP);
                    proxiedDP.setData(buffer,0,proxiedDP.getLength());
                    proxiedDP.setAddress(getResolvedAddress());
                    proxiedDP.setPort(getDestPort());
                    dsWrapper.getDs().send(proxiedDP);
                    //System.out.println("Источник -> Отправлен DDS'у");
                }
            } catch (Exception e) {
                setActive(false);
                System.err.println("Common DS-ACCEPTOR In err");
                e.printStackTrace();
            }
        },"DS-ACCEPTOR").start();
    }

    //synchronized private DSWrapper getDestDS(SocketAddress sa, InetAddress originAddress, int originPort) throws SocketException {
    synchronized private DSWrapper getDestDS(DatagramPacket dp) throws SocketException {
        Integer hash = calculateHash(dp.getAddress(),dp.getPort());

        DSWrapper dds = dsBinds.get(hash);
        if(dds == null) {
            // System.out.println("DDS создан "+dp.getAddress()+":"+dp.getPort());
            dds = new DSWrapper(new DatagramSocket(), serverDS, dp.getAddress(), dp.getPort());
            dsBinds.put(hash,dds);
        }
        dds.makeActive();
        //System.out.println("DDS найден "+dds.getDs().getLocalAddress()+":"+dds.getDs().getLocalPort());
        return dds;
    }
    private int calculateHash(InetAddress address, int port) {
        int hash = Objects.hash(Arrays.hashCode(address.getAddress()), port);
        // System.out.println("HASH FOR "+address+":"+port+" - "+hash);
        return hash;
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
