package info.kgeorgiy.ja.Ignatov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.*;

public class HelloUDPNonblockingServer implements HelloServer {
    private InnerHello helloServer;
    private final ConcurrentLinkedQueue<ByteBuffer> receiving = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Packet> sending = new ConcurrentLinkedQueue<>();

    public static void main(String[] args) {
        CommonServer.runApplication(args);
    }

    @Override
    public void start(int port, int threads) {
        if (helloServer != null) {
            return;
        }
        try {
            helloServer = new InnerHello(threads);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        try {
            registerChannel(port);
        } catch (IOException e) {
            System.err.println("Errors occurred while a registration datagram channel with a selector. " + e.getMessage());
            return;
        }

        var socket = helloServer.datagramChannel.socket();
        for (int i = 0; i < threads; i++) {
            try {
                receiving.add(ByteBuffer.allocate(socket.getReceiveBufferSize()));
            }catch (SocketException e){
                System.err.println(e.getMessage());
            }
        }
        helloServer.getBeginner().submit(() -> server(socket));

    }

    @Override
    public void close() {
        receiving.clear();
        sending.clear();
        helloServer.close();
    }

    private void registerChannel(int port) throws IOException {
        helloServer.datagramChannel.configureBlocking(false);
        helloServer.datagramChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        helloServer.datagramChannel.bind(new InetSocketAddress(port));
        helloServer.datagramChannel.register(helloServer.selector, SelectionKey.OP_READ);
    }

    private void server(DatagramSocket socket) {
        while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
            try {
                helloServer.selector.select(); // block
            } catch (IOException e) {
                System.err.println("I/O Errors occurred while selecting channels. " + e.getMessage());
            }
            for (final Iterator<SelectionKey> i = helloServer.selector.selectedKeys().iterator(); i.hasNext(); ) {
                final SelectionKey key = i.next();
                try {
                    if (!key.isValid()) {
                        continue;
                    }
                    helloServer.getExecutors().submit(() -> handle(key));
                } finally {
                    i.remove();
                }
            }
        }
    }
    private void handle(SelectionKey key) {
        final DatagramChannel datagramChannel = (DatagramChannel) key.channel();
        if (key.isReadable()) {
            if (!receiving.isEmpty()) {
                final ByteBuffer buffer = receiving.poll();
                if (receiving.isEmpty()) {
                    key.interestOpsAnd(~SelectionKey.OP_READ);
                    helloServer.selector.wakeup();
                }
                buffer.clear();
                try {
                    final SocketAddress address = datagramChannel.receive(buffer);
                    final byte[] message = makeServerMessage(buffer);
                    sending.add(new Packet(ByteBuffer.wrap(message), address));
                    key.interestOpsOr(SelectionKey.OP_WRITE);
                    helloServer.selector.wakeup();
                } catch (IOException e) {
                    System.err.println("I/O errors occurred while receiving the client message.");
                }
            }

        }
        if (key.isWritable()) {
            if (!sending.isEmpty()) {
                Packet packet = sending.poll();
                if (sending.isEmpty()) {
                    key.interestOpsAnd(~SelectionKey.OP_WRITE);
                    helloServer.selector.wakeup();
                }

                try {
                    datagramChannel.send(packet.buffer, packet.address);
                } catch (IOException e) {
                    System.err.println("I/O errors occurred while sending the response message.");
                }
                packet.buffer.clear();
                receiving.add(packet.buffer);
                key.interestOpsOr(SelectionKey.OP_READ);
                helloServer.selector.wakeup();
            }
        }

    }

    private byte[] makeServerMessage(ByteBuffer buffer) {
        byte[] clientMessage = Arrays.copyOfRange(buffer.array(), 0, buffer.position());
        return ("Hello, " + new String(clientMessage, CommonServer.CHARSET)).getBytes(CommonServer.CHARSET);
    }

    private static class InnerHello {
        private final Selector selector;
        private final DatagramChannel datagramChannel;
        private final ThreadPool threadPool;

        private InnerHello(int threads) throws IOException {
            try {
                selector = Selector.open();
            } catch (IOException e) {
                throw new IOException("Errors occurred while opening a selector. " + e.getMessage());
            }
            try {
                datagramChannel = DatagramChannel.open();
            } catch (IOException e) {
                throw new IOException("Errors occurred while opening a datagram channel. " + e.getMessage());
            }
            threadPool = new ThreadPool(threads);
        }

        private void close() {
            try {
                datagramChannel.close();
            } catch (IOException e) {
                System.err.println("I/O errors occurred while closing a datagram channel " + e.getMessage());
            }
            try {
                selector.close();
            } catch (IOException e) {
                System.err.println("I/O errors occurred while closing a selector. " + e.getMessage());
            }
            threadPool.close();
        }
        private ExecutorService getBeginner(){
            return threadPool.getBeginner();
        }
        protected ExecutorService getExecutors(){
            return threadPool.getExecutors();
        }
    }

    private record Packet(ByteBuffer buffer, SocketAddress address) {
    }

}
