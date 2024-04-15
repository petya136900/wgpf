package cf.furs.wgpf.packets;

public class HelloResponse extends AbstractPacket {
    public HelloResponse() {
        this.setPacketType(PacketType.HELLO_RESPONSE);
    }
}
