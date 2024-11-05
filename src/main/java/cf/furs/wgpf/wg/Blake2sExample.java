package cf.furs.wgpf.wg;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

public class Blake2sExample {
    public static void main(String[] args) throws DecoderException {
        String serverPubKeyB64 = "*serverpkb64*"; // We don't need the server keys, it's just for verification. The packet is 148 bytes long vs. 92 bytes from the handshake response
        String clientPubKeyB64 = "*clientpkb64*";

        byte[] serverPubKey = Base64.decodeBase64(serverPubKeyB64);
        byte[] clientPubKey = Base64.decodeBase64(clientPubKeyB64);

        byte[] label = "mac1----".getBytes();

        String handshakeInitiatorDataHex = "01000000b1390f96b9fa6ac9035e4fab63dfbdbae3ae4c82df006652ee5aa01700ff80e3bdc8b85da813fdda0933c33dfe093010deefbf233ea73c3fcae1bc98e0f5b8f85bb184d66e078d1015c4eae939087aab8d249ed8aa307b26eb59a3a68f526ae4d4494b1f1ecdd0cb7b13e4ee7fb2ff23";
        String handshakeResponseDataHex = "0200000034cb1303dc916b355828221e3e43cfd0fa0f278bb12b06783db919bf266a8bb2e7d9d4e295400626590ce1bee7301d7d30cc1b87575aa2a7";

        byte[] handshakeInitiatorData = Hex.decodeHex(handshakeInitiatorDataHex.toCharArray());
        byte[] handshakeResponseData = Hex.decodeHex(handshakeResponseDataHex.toCharArray());

        byte[] mac1KeyServer = blake2s256(label, serverPubKey);
        byte[] mac1KeyClient = blake2s256(label, clientPubKey);

        byte[] serverMAC1Initiator = blake2s128(mac1KeyServer, handshakeInitiatorData);
        byte[] clientMAC1Response = blake2s128(mac1KeyClient, handshakeResponseData);

        System.out.println("MAC1 for Handshake Initiator: " + Hex.encodeHexString(serverMAC1Initiator));
        System.out.println("MAC1 for Handshake Response: " + Hex.encodeHexString(clientMAC1Response));
    }

    public static byte[] blake2s256(byte[] label, byte[] pubKey) {
        Blake2s digest = new Blake2s(32, null); // 32 bytes for BLAKE2s-256
        digest.update(label);
        digest.update(pubKey);
        return digest.digest();
    }

    public static byte[] blake2s128(byte[] key, byte[] data) {
        Blake2s digest = new Blake2s(16, key); // 16 bytes for BLAKE2s-128
        digest.update(data);
        return digest.digest();
    }
}
