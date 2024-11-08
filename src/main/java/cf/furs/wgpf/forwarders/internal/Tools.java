package cf.furs.wgpf.forwarders.internal;

import java.util.Base64;

public class Tools {

    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static byte[] decodeBase64(String b64String) {
        return Base64.getDecoder().decode(b64String);
    }

    public static byte[] decodeHex(String hexString) {
        if (hexString.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid hex string length");
        }

        int len = hexString.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            int highNibble = Character.digit(hexString.charAt(i), 16);
            int lowNibble = Character.digit(hexString.charAt(i + 1), 16);

            if (highNibble == -1 || lowNibble == -1) {
                throw new IllegalArgumentException("Invalid hex character detected");
            }

            data[i / 2] = (byte) ((highNibble << 4) + lowNibble);
        }

        return data;
    }
}
