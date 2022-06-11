package info.kgeorgiy.ja.Ignatov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Arrays;
import java.util.Iterator;


public class HelloUDPNonblockingClient implements HelloClient {
    private static final int QUERY_TIMEOUT = 20;


    public static void main(String[] args) {
        CommonClient.runApplication(args);
    }

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        Selector selector;
        try {
            selector = Selector.open();
        } catch (IOException e) {
            System.err.println("Errors occurred while opening a selector");
            return;
        }
        for (int i = 0; i < threads; i++) {
            registerChannel(host, port, selector, i);
        }
        while (!selector.keys().isEmpty() && !Thread.currentThread().isInterrupted()) {
            try {
                selector.select(QUERY_TIMEOUT); // block
            } catch (IOException e) {
                System.err.println("I/O Errors occurred while selecting channels. " + e.getMessage());
            }
            if (!selector.selectedKeys().isEmpty()) {
                for (final Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext(); ) {
                    final SelectionKey key = i.next();
                    try {
                        if (!key.isValid()) {
                            continue;
                        }
                        try {
                            handle(prefix, requests, key);
                        } catch (IOException e) {
                            selector.keys().forEach(k -> k.interestOps(SelectionKey.OP_WRITE));
                        }
                    } finally {
                        i.remove();
                    }
                }
            } else {
                selector.keys().forEach(key -> key.interestOps(SelectionKey.OP_WRITE));
            }
        }
        selector.keys().forEach(key -> {
            try {
                key.channel().close();
            } catch (IOException e) {
                System.err.println("Errors occurred while closing some channels");
            }
        });
        try {
            selector.close();
        } catch (IOException e) {
            System.err.println("Errors occurred while closing some channels");
        }
    }

    private void handle(String prefix, int requests, SelectionKey key) throws IOException {
        final DatagramChannel datagramChannel = (DatagramChannel) key.channel();
        final ChannelAttachedInfo channelInfo = (ChannelAttachedInfo) key.attachment();
        String requestMessage = CommonClient.makeRequest(prefix, channelInfo.id, channelInfo.requestNumber);

        if (key.isWritable()) {
            channelInfo.buffer.clear();
            channelInfo.buffer.put(requestMessage.getBytes(CommonClient.CHARSET));
            channelInfo.buffer.flip();
            datagramChannel.send(channelInfo.buffer,
                    datagramChannel.getRemoteAddress());
            key.interestOps(SelectionKey.OP_READ);
        }

        if (key.isReadable()) {
            channelInfo.buffer.clear();
            datagramChannel.receive(channelInfo.buffer);
            String responseMessage = new String(Arrays.copyOfRange(channelInfo.buffer.array(), 0,
                    channelInfo.buffer.position()), CommonClient.CHARSET);
            if (CommonClient.isSuccessfulRequest(responseMessage, requestMessage)) {
                CommonClient.printMessage(responseMessage, requestMessage);
                channelInfo.requestNumber++;
                if (requests <= channelInfo.requestNumber) {
                    datagramChannel.close();
                } else {
                    key.interestOps(SelectionKey.OP_WRITE);
                }
            }
        }

    }

    private void registerChannel(String host, int port, Selector selector, int id) {
        DatagramChannel datagramChannel;
        try {
            datagramChannel = DatagramChannel.open();
            datagramChannel.configureBlocking(false);
            datagramChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            datagramChannel.connect(new InetSocketAddress(host, port));
            datagramChannel.register(selector, SelectionKey.OP_WRITE,
                    new ChannelAttachedInfo(datagramChannel.socket().getReceiveBufferSize(), id));
        } catch (IOException e) {
            System.err.println("I/O errors occurred while registration a new datagram channel " +
                    "with a selector. " + e.getMessage());
        }

    }

    private static class ChannelAttachedInfo {
        private final ByteBuffer buffer;
        private final int id;
        private int requestNumber = 0;

        private ChannelAttachedInfo(int size, int id) {
            buffer = ByteBuffer.allocate(size);
            this.id = id;
        }
    }
}
