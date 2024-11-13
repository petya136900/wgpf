package cf.furs.wgpf.internal;

import cf.furs.wgpf.forwarders.internal.Tools;
import cf.furs.wgpf.forwarders.internal.WGPacket;

import java.io.*;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import static cf.furs.wgpf.wg.Blake2sExample.blake2s128;
import static cf.furs.wgpf.wg.Blake2sExample.blake2s256;

public class DeferredWGRPAnalyze {
    private final LinkedBlockingQueue<WGPacket> uncheckedRawPackets = new LinkedBlockingQueue<>();
    private final ConcurrentHashMap<Long, WGPacket> rawPacketMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> uniqCounts = new ConcurrentHashMap<>();
    private volatile boolean running = false;
    private Thread analyzerThread;
    private List<String> peerPublicKeys;
    private static File logFile;

    private final ConcurrentMap<String, Boolean> uniqPeers = new ConcurrentHashMap<>();

    private static final byte[] LABEL = "mac1----".getBytes();

    // Метод для загрузки публичных ключей из файла
    private void loadPeerPublicKeys() throws IOException {
        File publicKeyFile = new File("public_keys.txt");
        if (!publicKeyFile.exists()) {
            throw new FileNotFoundException("The public_keys.txt file is not found in the current directory");
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(publicKeyFile))) {
            peerPublicKeys = reader.lines().collect(Collectors.toList());
        }
    }


    public void addToQueue(WGPacket wgPacket) {
        long key = System.currentTimeMillis();
        // System.out.println(bytesToHex(wgPacket.getData()));
        uncheckedRawPackets.offer(wgPacket);
        rawPacketMap.put(key, wgPacket);
    }

//    public void startAnalyze() throws IOException {
      public void startAnalyze() {
        if (this.running) return;
        String homeDirectory = System.getProperty("user.home");
        File logDir = new File(homeDirectory, "wg_peer_log");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        logFile = new File(logDir, "log.txt");
        System.out.println("logFile: " + logFile.getAbsolutePath());
        try {
            loadPeerPublicKeys();
        } catch (Exception e) {
            System.err.println("Failed to load public_keys, DWGRP stopped..");
            return;
        }
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
            String uniqString = publicKey+"_"+wgPacket.getAddress().getHostAddress();
            boolean isUniq = (uniqPeers.putIfAbsent(uniqString, true) == null);
            if (checkMac(mac1, publicKey, Arrays.copyOfRange(packet, 0, 60))) {
                logPeer(publicKey, wgPacket, isUniq, wgPacket.getDestHost(), wgPacket.getDestPort());
                return true;
            }
        }
        String uniqString = "Unknown"+"_"+wgPacket.getAddress().getHostAddress();
        boolean isUniq = (uniqPeers.putIfAbsent(uniqString, true) == null);
        logUnknownPeer("Unknown", wgPacket, isUniq, wgPacket.getDestHost(), wgPacket.getDestPort());
        return false;
    }

    private boolean checkMac(byte[] mac1, String publicKeyb64, byte[] data) {
        try {
            // System.out.println("\n\nMAC1: " + bytesToHex(mac1));
            // System.out.println("publicKeyb64: " + publicKeyb64);
            // System.out.println("data: " + bytesToHex(data));
            byte[] pubKey = Tools.decodeBase64(publicKeyb64);
            byte[] mac1Key = blake2s256(LABEL, pubKey);
            byte[] MAC1Response = blake2s128(mac1Key, data);
            // System.out.println("mac1 for this PK: " + bytesToHex(MAC1Response));
            return Arrays.equals(mac1, MAC1Response);
        } catch (Exception ignored) {
            return false;
        }
    }

    private void logPeer(String publicKey, WGPacket wgPacket, boolean isUniq, String destHost, int destPort) {
        try {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                writer.write(String.format("[TO %s:%d] | [%s] PublicKey %s - %s:%d%s%n",
                        destHost,
                        destPort,
                        timestamp,
                        publicKey,
                        wgPacket.getAddress().getHostAddress(),
                        wgPacket.getPort(),(isUniq?" - NEW UNIQ CONN["+getAndInc(publicKey)+"]":"")));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void logUnknownPeer(String publicKey, WGPacket wgPacket, boolean isUniq, String destHost, int destPort) {
        try {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                writer.write(String.format("[TO %s:%d] | [%s] PublicKey %s - %s:%d%s%n",
                        destHost,
                        destPort,
                        timestamp,
                        publicKey,
                        wgPacket.getAddress().getHostAddress(),
                        wgPacket.getPort(),(isUniq?" - NEW UNIQ CONN["+getAndInc(publicKey)+"]":"")));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized int getAndInc(String uniqString) {
        AtomicInteger ai = uniqCounts.getOrDefault(uniqString, new AtomicInteger(0));
        int tmpInt = ai.incrementAndGet();
        uniqCounts.put(uniqString, ai);
        return tmpInt;
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
