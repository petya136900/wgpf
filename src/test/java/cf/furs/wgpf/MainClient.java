package cf.furs.wgpf;

import cf.furs.wgpf.packets.AbstractPacket;
import cf.furs.wgpf.packets.PacketType;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class MainClient {
    public static void main(String[] args) throws IOException {
        byte[] data = ByteBuffer
                .allocate(Integer.BYTES * 3)
                .putInt(PacketType.HELLO_REQUEST.ordinal())
                .putInt(PacketType.RESERVED_ZERO.ordinal())
                .putInt(PacketType.HELLO_RESPONSE.ordinal()).array();
        DatagramPacket dp =
                new DatagramPacket(data, data.length, new InetSocketAddress("localhost", 11212));

        try (DatagramSocket clientDS = new DatagramSocket()) {
            clientDS.send(dp);
            System.out.println("SENT");
            clientDS.receive(dp);
            AbstractPacket p = OriginUDPServer.getPacket(dp);
            System.out.println("RECEIVED: " + p.getPacketType());
        }
    }
}
