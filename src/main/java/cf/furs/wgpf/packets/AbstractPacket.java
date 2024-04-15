package cf.furs.wgpf.packets;

public abstract class AbstractPacket {
    private PacketType packetType;
    protected AbstractPacket() {    }
    private byte[] data;
    public byte[] getData() {
        return data;
    }

    public PacketType getPacketType() {
        return packetType;
    }

    protected AbstractPacket setPacketType(PacketType packetType) {
        this.packetType = packetType;
        return this;
    }

    public AbstractPacket setData(byte[] data) {
        this.data = data;
        return this;
    }
}
