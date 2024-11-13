package cf.furs.wgpf.forwarders.internal;

import cf.furs.wgpf.forwarders.ForwarderUDP;
import cf.furs.wgpf.internal.MagicFunction;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

public class DSWrapper {
    public static final long IDLE_TIMEOUT_C = 60;
    private final DatagramSocket serverDS;
    private final InetAddress bindedRemoteIA;
    private final Integer bindedPort;
    private final DatagramSocket ds;
    private long idleTime = 0;
    private final Thread readThread;
    private boolean isActive = true;

    private String destHost;
    private int destPort;

    public String getDestHost() {
        return destHost;
    }

    public void setDestHost(String destHost) {
        this.destHost = destHost;
    }

    public int getDestPort() {
        return destPort;
    }

    public void setDestPort(int destPort) {
        this.destPort = destPort;
    }

    private int serverShift = 0;
    private MagicFunction magicFunction = null;

    public int getServerShift() {
        return serverShift;
    }

    public void setServerShift(int serverShift) {
        this.serverShift = serverShift;
    }

    public MagicFunction getMagicFunction() {
        return magicFunction;
    }

    public void setMagicFunction(MagicFunction magicFunction) {
        this.magicFunction = magicFunction;
    }

    public DSWrapper(DatagramSocket ds, DatagramSocket serverDS, InetAddress bindedRemoteIA, Integer bindedPort) {
        // System.out.println("Новый DDS! - " + ds.getLocalAddress()+":"+ds.getLocalPort());
        this.ds = ds;
        this.serverDS = serverDS;
        this.bindedRemoteIA = bindedRemoteIA;
        this.bindedPort = bindedPort;
        readThread = new Thread(()->{
            if(this.magicFunction==null)
                serverShift=0;
            try {
                byte[] buffer = new byte[ForwarderUDP.UDP_FRAME_BUFFER_SIZE];
                while (isActive) {
                    DatagramPacket proxiedDP = new DatagramPacket(buffer, buffer.length);
                    ds.receive(proxiedDP);
                    //System.out.println("Новый пакет для нашего DDS! - " + ds.getLocalAddress()+":"+ds.getLocalPort());
                    if(this.serverShift!=0) {
                        this.magicFunction.magic(buffer,proxiedDP.getLength(),this.serverShift);
                    }
                    // proxiedDP.setData(buffer, 0, proxiedDP.getLength());
                    if(proxiedDP.getLength()==92) {
                        // System.out.println("New Init Response packet for  - " + ds.getLocalAddress()+":"+ds.getLocalPort());
                        ForwarderUDP.wgrpAnalyzer.addToQueue(
                                new WGPacket(
                                        bindedRemoteIA,
                                        bindedPort,
                                        Arrays.copyOf(buffer, proxiedDP.getLength()))
                                            .setDestHost(
                                                    this.getDestHost()).
                                            setDestPort(
                                                    this.getDestPort()));
                    }
                    proxiedDP.setAddress(getBindedRemoteIA());
                    proxiedDP.setPort(getBindedPort());
                    this.serverDS.send(proxiedDP);
                    //System.out.println("От DDS -> Отправлен источнику");
                }
            } catch (Exception e) {
                // System.err.println("Common Ds In Err");
            }
        },"DS-READER [" + bindedRemoteIA + "]");
        readThread.start();
    }
    public Integer getBindedPort() {
        return bindedPort;
    }
    public InetAddress getBindedRemoteIA() {
        return bindedRemoteIA;
    }
    public DatagramSocket getDs() {
        return ds;
    }
    public boolean isIdle() {
        return ++idleTime > IDLE_TIMEOUT_C;
    }
    public DSWrapper makeActive() {
        this.idleTime = 0;
        return this;
    }
    public void destroy() {
        this.isActive = false;
        this.readThread.interrupt();
    }
}
