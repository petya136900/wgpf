package cf.furs.wgpf.internal;

import cf.furs.wgpf.forwarders.internal.WGPacket;
import org.apache.commons.codec.binary.Base64;

import java.io.*;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static cf.furs.wgpf.forwarders.internal.Tools.bytesToHex;
import static cf.furs.wgpf.wg.Blake2sExample.blake2s128;
import static cf.furs.wgpf.wg.Blake2sExample.blake2s256;

public class DeferredWGRPAnalyze {
    private final LinkedBlockingQueue<WGPacket> uncheckedRawPackets = new LinkedBlockingQueue<WGPacket>();
    private final ConcurrentHashMap<Long, WGPacket> rawPacketMap = new ConcurrentHashMap<>();
    private volatile boolean running = false;
    private Thread analyzerThread;
    private List<String> peerPublicKeys;
    private static File logFile;

    private static final byte[] LABEL = "mac1----".getBytes();

    // Метод для загрузки публичных ключей из файла
    private void loadPeerPublicKeys() throws IOException, FileNotFoundException {
        File publicKeyFile = new File("public_keys.txt");
        // System.out.println("publicKeyFile: " + publicKeyFile.getAbsolutePath());
        if (!publicKeyFile.exists()) {
            throw new FileNotFoundException("Файл public_keys.txt не найден в текущей директории");
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(publicKeyFile))) {
            peerPublicKeys = reader.lines().toList();
        }
    }

    public void addToQueue(WGPacket wgPacket) {
        long key = System.currentTimeMillis();
        // System.out.println(bytesToHex(wgPacket.getData()));
        uncheckedRawPackets.offer(wgPacket);
        rawPacketMap.put(key, wgPacket);
    }

    public void startAnalyze() throws IOException {
        if (this.running) return;
        String homeDirectory = System.getProperty("user.home");
        File logDir = new File(homeDirectory, "wg_peer_log");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        logFile = new File(logDir, "log.txt");
        System.out.println("logFile: " + logFile.getAbsolutePath());
        loadPeerPublicKeys();
        System.out.println("DWGRP running..");
        running = true;
        analyzerThread = new Thread(() -> {
            while (running) {
                try {
                    WGPacket packet = uncheckedRawPackets.poll(100, TimeUnit.MILLISECONDS);
                    if (packet != null) {
                        // System.out.println("Checking packet..");
                        if (check(packet)) {
                            // System.out.println("The packet has been successfully verified and logged.");
                        } else {
                            // System.out.println("The packet does not fit any of the PKs);
                        }
                        rawPacketMap.values().remove(packet);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        analyzerThread.start();
    }

    public void stopAnalyze() {
        running = false;
        if (analyzerThread != null) {
            analyzerThread.interrupt();
            try {
                analyzerThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean check(WGPacket wgPacket) {
        byte[] packet = wgPacket.getData();
        if (packet.length != 92) {
            return false;
        }
        if (packet[0] != 0x02 || packet[1] != 0x00 || packet[2] != 0x00 || packet[3] != 0x00) {
            return false;
        }
        byte[] mac1 = Arrays.copyOfRange(packet, 60, 76);

        for (String publicKey : peerPublicKeys) {
            if (checkMac(mac1, publicKey, Arrays.copyOfRange(packet, 0, 60))) {
                logPeer(publicKey, wgPacket);
                return true;
            }
        }
        return false;
    }

    private boolean checkMac(byte[] mac1, String publicKeyb64, byte[] data) {
        // System.out.println("\n\nMAC1: " + bytesToHex(mac1));
        // System.out.println("publicKeyb64: " + publicKeyb64);
        // System.out.println("data: " + bytesToHex(data));
        byte[] pubKey = Base64.decodeBase64(publicKeyb64);
        byte[] mac1Key = blake2s256(LABEL, pubKey);
        byte[] MAC1Response = blake2s128(mac1Key, data);
        // System.out.println("mac1 for this PK: " + bytesToHex(MAC1Response));
        return Arrays.equals(mac1, MAC1Response);
    }

    private void logPeer(String publicKey, WGPacket wgPacket) {
        try {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                writer.write(String.format("[%s] PublicKey %s - %s:%d%n", timestamp, publicKey, wgPacket.getAddress().getHostAddress(),wgPacket.getPort()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        DeferredWGRPAnalyze analyzer = new DeferredWGRPAnalyze();
        analyzer.startAnalyze();

        analyzer.addToQueue(new WGPacket(InetAddress.getByName("localhost"),123,new byte[]{1, 2, 3}));
        analyzer.addToQueue(new WGPacket(InetAddress.getByName("localhost"),123,new byte[]{4, 5, 6}));

        Runtime.getRuntime().addShutdownHook(new Thread(analyzer::stopAnalyze));

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        analyzer.stopAnalyze();
    }
}
