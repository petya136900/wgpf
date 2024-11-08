package cf.furs.wgpf;

import cf.furs.wgpf.forwarders.ForwarderTCP;
import cf.furs.wgpf.forwarders.ForwarderUDP;

import java.net.UnknownHostException;

public class MainServer {
    public static void main(String[] args) throws UnknownHostException {

        //  new ForwarderTCPServer(12345, "192.168.51.65", 3389, 2)
        //    .startThread();
        // new ForwarderUDP(12345, "wg-endpoint.xyz", 12345, 2)
        //    .startThread();

        if (args.length < 4 || args[0].equals("--help")) {
            System.out.println("Usage:");
            System.out.println("For TCP: java -jar my.jar tcp <listenPort> <destHost> <destPort> [forwarderType]");
            System.out.println("For UDP: java -jar my.jar udp <listenPort> <destHost> <destPort> [magic-type] [client-shift] [server-shift]");
            return;
        }

        String protocol = args[0];
        int listenPort = Integer.parseInt(args[1]);
        String destHost = args[2];
        int destPort = Integer.parseInt(args[3]);
        int forwarderType = args.length > 4 ? Integer.parseInt(args[4]) : 1;
        if (protocol.equalsIgnoreCase("tcp")) {
            new ForwarderTCP(listenPort, destHost, destPort, forwarderType).startThread();
        } else if (protocol.equalsIgnoreCase("udp")) {
            int clientShift = args.length > 4 ? Integer.parseInt(args[5]) : 0;
            int serverShift = args.length > 4 ? Integer.parseInt(args[6]) : 1;
            new ForwarderUDP(listenPort, destHost, destPort, forwarderType,clientShift,serverShift).startThread();
        } else {
            System.out.println("Invalid protocol. Please use 'tcp' or 'udp'.");
        }
    }
}
