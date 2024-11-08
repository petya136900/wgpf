package cf.furs.wgpf.forwarders;

import java.net.InetAddress;
import java.net.UnknownHostException;

public abstract class AbstractForwarder {
    private final Integer listPort;
    private final String destHost;
    private final Integer destPort;
    private Thread serverThread;
    private Boolean isActive = false;
    private Integer forwarderType;

    public Integer getForwarderType() {
        return forwarderType;
    }

    public void setForwarderType(Integer forwarderType) {
        this.forwarderType = forwarderType;
    }

    public void setServerThread(Thread serverThread) {
        this.serverThread = serverThread;
    }

    public Thread getServerThread() {
        return serverThread;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }
    private InetAddress resolvedAddress;

    public InetAddress getResolvedAddress() {
        return resolvedAddress;
    }

    public void setResolvedAddress(InetAddress resolvedAddress) {
        this.resolvedAddress = resolvedAddress;
    }

    protected AbstractForwarder(Integer listPort, String destHost, Integer destPort, int forwarderType) throws UnknownHostException {
        this.listPort = listPort;
        this.destHost = destHost;
        this.destPort = destPort;
        this.setResolvedAddress(InetAddress.getByName(this.getDestHost()));
        this.setForwarderType(forwarderType);
    }
    public boolean serverIsActive() {
        return isActive;
    }

    public Integer getListPort() {
        return listPort;
    }

    public String getDestHost() {
        return destHost;
    }

    public Integer getDestPort() {
        return destPort;
    }
    public abstract void startThread();
}
