package cf.furs.wgpf;

import cf.furs.wgpf.internal.DeferredWGRPAnalyze;
import cf.furs.wgpf.packets.AbstractPacket;
import cf.furs.wgpf.packets.HelloPacket;
import cf.furs.wgpf.packets.HelloResponse;
import cf.furs.wgpf.packets.PacketType;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class OriginUDPServer {
    private int listPort;
    private DatagramSocket serverDatagramSocket;
    boolean serverIsActive = false;

    public static void main(String[] args) throws IOException {
        OriginUDPServer originUDPServer = new OriginUDPServer();
        originUDPServer.start(1234);
    }

    private static final int PACKET_SIZE = 1024;
    private synchronized void start(int listPort) throws IOException {
        this.listPort = listPort;
        this.serverDatagramSocket = new DatagramSocket(this.listPort);
        this.serverIsActive = true;
        byte[] buf = new byte[PACKET_SIZE];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        while(serverIsActive) {
            serverDatagramSocket.receive(packet);
            AbstractPacket mPacket = getPacket(packet);
            if(mPacket!=null) {
                System.out.println("Gotcha new " + mPacket.getPacketType() + " packet!\n\tData: " + mPacket.getData().length + " bytes\n\t" + Arrays.toString(mPacket.getData()));
                if(mPacket.getPacketType().equals(PacketType.HELLO_REQUEST)) {
                    packet.setData(ByteBuffer
                            .allocate(Integer.BYTES * 3)
                            .putInt(PacketType.HELLO_RESPONSE.ordinal())
                            .putInt(1)
                            .putInt(2).array());
                    serverDatagramSocket.send(packet);
                }
            }
        }
    }

    public static AbstractPacket getPacket(DatagramPacket packet) {
        if (packet.getData().length < 4) {
            throw new IllegalArgumentException("Invalid UDP packet data length");
        }

        // Extract the first 4 bytes to determine the PacketType
        int packetTypeOrdinal = byteArrayToInt(Arrays.copyOfRange(packet.getData(), 0, 4));
        PacketType packetType = PacketType.values()[packetTypeOrdinal];

        AbstractPacket aPacket = null;
        switch (packetType) {
            case RESERVED_ZERO:
            case HELLO_REQUEST:
                aPacket = new HelloPacket().setData(Arrays.copyOfRange(packet.getData(), 4, packet.getLength()));
                return aPacket;
            case HELLO_RESPONSE:
                aPacket = new HelloResponse().setData(Arrays.copyOfRange(packet.getData(), 4, packet.getLength()));
                return aPacket;
            default:
                System.out.println("Unknown packet Type: "+packetType);
        }
        return null;
    }
    private static int byteArrayToInt(byte[] bytes) {
        return bytes[3] & 0xFF |
                (bytes[2] & 0xFF) << 8 |
                (bytes[1] & 0xFF) << 16 |
                (bytes[0] & 0xFF) << 24;
    }
}
