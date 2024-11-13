package cf.furs.wgpf.forwarders.internal;

import java.net.InetAddress;

public class WGPacket {
    private InetAddress address;
    private int port;
    private byte[] data;

    private String destHost;
    private int destPort;

    public String getDestHost() {
        return destHost;
    }

    public WGPacket setDestHost(String destHost) {
        this.destHost = destHost;
        return this;
    }

    public int getDestPort() {
        return destPort;
    }

    public WGPacket setDestPort(int destPort) {
        this.destPort = destPort;
        return this;
    }

    public WGPacket(InetAddress address, int port, byte[] data) {
        this.address = address;
        this.port = port;
        this.data = data;
    }

    public InetAddress getAddress() {
        return address;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
