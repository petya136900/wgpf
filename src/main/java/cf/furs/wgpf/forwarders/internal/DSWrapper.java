package cf.furs.wgpf.forwarders.internal;

import cf.furs.wgpf.forwarders.ForwarderUDP;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

import static cf.furs.wgpf.forwarders.internal.Tools.bytesToHex;

public class DSWrapper {
    public static final long IDLE_TIMEOUT_C = 60;
    private final DatagramSocket serverDS;
    private final InetAddress bindedRemoteIA;
    private final Integer bindedPort;
    private final DatagramSocket ds;
    private long idleTime = 0;
    private final Thread readThread;
    private boolean isActive = true;
    public DSWrapper(DatagramSocket ds, DatagramSocket serverDS, InetAddress bindedRemoteIA, Integer bindedPort) {
        // System.out.println("Новый DDS! - " + ds.getLocalAddress()+":"+ds.getLocalPort());
        this.ds = ds;
        this.serverDS = serverDS;
        this.bindedRemoteIA = bindedRemoteIA;
        this.bindedPort = bindedPort;
        readThread = new Thread(()->{
            try {
                byte[] buffer = new byte[ForwarderUDP.UDP_FRAME_BUFFER_SIZE];
                while (isActive) {
                    DatagramPacket proxiedDP = new DatagramPacket(buffer, buffer.length);
                    ds.receive(proxiedDP);
                    //System.out.println("Новый пакет для нашего DDS! - " + ds.getLocalAddress()+":"+ds.getLocalPort());
                    proxiedDP.setData(buffer, 0, proxiedDP.getLength());
                    if(proxiedDP.getLength()==92) {
                        // System.out.println("New Init Response packet for  - " + ds.getLocalAddress()+":"+ds.getLocalPort());
                        ForwarderUDP.wgrpAnalyzer.addToQueue(new WGPacket(ds.getLocalAddress(),ds.getLocalPort(),Arrays.copyOf(buffer, proxiedDP.getLength())));
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
