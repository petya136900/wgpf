package cf.furs.wgpf.forwarders.internal;

import java.net.InetAddress;

public class WGPacket {
    private InetAddress address;
    private int port;
    private byte[] data;

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
