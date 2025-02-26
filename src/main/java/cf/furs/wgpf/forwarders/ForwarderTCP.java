package cf.furs.wgpf.forwarders;

import cf.furs.wgpf.forwarders.internal.StreamThrough;
import cf.furs.wgpf.forwarders.internal.ThreadPoolManager;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ForwarderTCP extends AbstractForwarder {
    private static final int TCP_FRAME_BUFFER_SIZE = 4096 * 16;
    private ServerSocket serverSocket;
    private StreamThrough st = null;
    ThreadPoolManager manager = null;
    public ForwarderTCP(Integer listPort, String destHost, Integer destPort, int forwarderType) throws UnknownHostException {
        super(listPort, destHost, destPort, forwarderType);
    }
    @Override
    public void startThread() {
        manager = new ThreadPoolManager();
        new Thread(()->{
            try {
                this.setActive(true);
                this.setServerThread(Thread.currentThread());
                this.serverSocket = new ServerSocket(this.getListPort());
                switch(this.getForwarderType()) {
                    case(1):
                        this.st = (in,out)->{
                            ReadableByteChannel inputChannel = Channels.newChannel(in);
                            WritableByteChannel outputChannel = Channels.newChannel(out);
                            ByteBuffer buffer = ByteBuffer.allocate(TCP_FRAME_BUFFER_SIZE);
                            while (inputChannel.read(buffer) != -1) {
                                buffer.flip();
                                outputChannel.write(buffer);
                                buffer.clear();
                            }
                            inputChannel.close();
                            outputChannel.close();
                        };
                        break;
                    case(2):
                        this.st = (in,out)->{
                            byte[] buffer = new byte[TCP_FRAME_BUFFER_SIZE];
                            int bytesRead;
                            while ((bytesRead = in.read(buffer)) != -1) {
                                out.write(buffer, 0, bytesRead);
                            }
                            out.flush(); // Очистка буфера OutputStream
                        };
                        break;
                    case(3):
                        this.st = (in,out)->{
                            ReadableByteChannel inputChannel = Channels.newChannel(in);
                            WritableByteChannel outputChannel = Channels.newChannel(out);
                            ByteBuffer buffer = ByteBuffer.allocateDirect(TCP_FRAME_BUFFER_SIZE);
                            while (inputChannel.read(buffer) != -1 || buffer.position() > 0) {
                                buffer.flip();
                                outputChannel.write(buffer);
                                buffer.compact();
                            }
                        };
                        break;
                    case(4):
                        this.st = (in,out)->{
                            int data;
                            while ((data = in.read()) != -1) {
                                out.write(data);
                            }
                            out.flush();
                        };
                        break;
                    case(5):
                        this.st = (in,out)->{
                            try (BufferedInputStream bufferedInput = new BufferedInputStream(in);
                                 BufferedOutputStream bufferedOutput = new BufferedOutputStream(out)) {
                                byte[] buffer = new byte[TCP_FRAME_BUFFER_SIZE];
                                int bytesRead;
                                while ((bytesRead = bufferedInput.read(buffer)) != -1) {
                                    bufferedOutput.write(buffer, 0, bytesRead);
                                }
                                out.flush();
                            }
                        };
                        break;
                    case(6):
                        this.st = (in,out) -> {
                            try (DataInputStream dIn = new DataInputStream(in);
                                 DataOutputStream dOut = new DataOutputStream(out)) {
                                byte[] buffer = new byte[TCP_FRAME_BUFFER_SIZE];
                                int bytesRead;
                                while ((bytesRead = dIn.read(buffer)) != -1) {
                                    dOut.write(buffer, 0, bytesRead);
                                }
                            }
                        };
                        break;
                    default:
                        System.err.println("Unknown Forwarder Type (1-6 only available): "+getForwarderType());
                        System.exit(2);
                }

                while(this.serverIsActive()) {
                    Socket incomingSocket = serverSocket.accept();
                    manager.getAccepterPool().submit(() -> {
                        try {
                            final Socket destinationSocket = getNewDS(getResolvedAddress(), getDestPort());
                            manager.getReaderPool().submit(() -> {
                                try {
                                    st.forwardStream(incomingSocket.getInputStream(),destinationSocket.getOutputStream());
                                } catch (Exception e) {
                                    // System.err.println("Incoming Socket In err");
                                } finally {
                                    try {
                                        incomingSocket.close();
                                    } catch (IOException ignored) {}
                                }
                            });
                            manager.getWriterPool().submit(() -> {
                                try {
                                    st.forwardStream(destinationSocket.getInputStream(),incomingSocket.getOutputStream());
                                } catch (Exception e) {
                                    // System.err.println("Destination Socket In err");
                                } finally {
                                    try {
                                        destinationSocket.close();
                                    } catch (IOException ignored) {}
                                }
                            });
                        } catch (Exception ignored) {
                            try {
                                incomingSocket.close();
                            } catch (Exception ignored2) {}
                        }
                    });

                }
            } catch (Exception e) {
                this.setActive(false);
                this.getServerThread().interrupt();
                System.err.println("Common Socket err");
                e.printStackTrace();
            }
        },"SOCKET-ACCEPTOR").start();
    }

    private Socket getNewDS(final InetAddress resolvedAddress, final Integer destPort) throws IOException {
        return new Socket(resolvedAddress, destPort);
    }
}
