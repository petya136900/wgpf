package cf.furs.wgpf.packets;

public class HelloPacket extends AbstractPacket {
    public HelloPacket() {
        this.setPacketType(PacketType.HELLO_REQUEST);
    }
}
