package cf.furs.wgpf.internal;
import java.util.Arrays;
public class CoCoder {
    public static void shiftBytesInPlace(byte[] buffer, int length, int shift) {
        for (int i = 0; i < length; i++) {
            buffer[i] = (byte) (buffer[i] + shift);
        }
    }
    public static byte[] shiftBytesCopy(byte[] buffer, int length, int shift) {
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[i] = (byte) (buffer[i] + shift);
        }
        return result;
    }
    public static void rotateBytesInPlace(byte[] buffer, int length, int shift) {
        shift = shift % length;
        if (shift < 0) shift += length;

        reverse(buffer, 0, length - 1);
        reverse(buffer, 0, shift - 1);
        reverse(buffer, shift, length - 1);
    }
    public static byte[] rotateBytesCopy(byte[] buffer, int length, int shift) {
        shift = shift % length;
        if (shift < 0) shift += length;

        byte[] result = new byte[length];
        System.arraycopy(buffer, length - shift, result, 0, shift);
        System.arraycopy(buffer, 0, result, shift, length - shift);
        return result;
    }
    private static void reverse(byte[] array, int start, int end) {
        while (start < end) {
            byte temp = array[start];
            array[start] = array[end];
            array[end] = temp;
            start++;
            end--;
        }
    }
    public static MagicFunction getMagicFunc(int type) {
        if (type == 1) {
            return CoCoder::shiftBytesInPlace;
        } else if (type == 2) {
            return (buffer, length, shift) -> {
                byte[] result = CoCoder.shiftBytesCopy(buffer, length, shift);
                System.arraycopy(result, 0, buffer, 0, length);
            };
        } else if (type == 3) {
            return CoCoder::rotateBytesInPlace;
        } else if (type == 4) {
            return (buffer, length, shift) -> {
                byte[] result = CoCoder.rotateBytesCopy(buffer, length, shift);
                System.arraycopy(result, 0, buffer, 0, length);
            };
        } else {
            throw new IllegalArgumentException("Unsupported magic type: " + type);
        }
    }
    public static void main(String[] args) {
        byte[] originalBuffer = new byte[32];
        for (int i = 0; i < originalBuffer.length; i++) {
            originalBuffer[i] = (byte) i;
        }
        int packetLength = 8;
        int shiftValue = 1;
        for (int type = 1; type <= 4; type++) {
            System.out.println("\nTest magic type" + type + ":");
            byte[] bufferToTest = Arrays.copyOf(originalBuffer, originalBuffer.length);
            MagicFunction magicFunc = getMagicFunc(type);
            System.out.println("Origin packet: " + Arrays.toString(bufferToTest));
            magicFunc.magic(bufferToTest, packetLength, shiftValue);
            System.out.println("After magic +" + shiftValue + ": " + Arrays.toString(bufferToTest));
            magicFunc.magic(bufferToTest, packetLength, -shiftValue);
            System.out.println("After magic -" + shiftValue + ": " + Arrays.toString(bufferToTest));
            boolean testResult = Arrays.equals(
                    Arrays.copyOfRange(originalBuffer, 0, packetLength),
                    Arrays.copyOfRange(bufferToTest, 0, packetLength)
            );
            System.out.println("Is packet same?: " + testResult);
        }
    }
}
