package info.kgeorgiy.ja.Ignatov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.*;

/**
 * @author Ignatov Nikolay
 */

public class HelloUDPServer implements HelloServer {
    private InnerHello helloServer;

    public static void main(String[] args) {
        CommonServer.runApplication(args);
    }

    @Override
    public void start(int port, int threads) {
        try {
            if (helloServer != null) {
                return;
            }
            helloServer = new InnerHello(port, threads);
            helloServer.getBeginner().submit(this::server);
        } catch (SocketException e) {
            System.err.println("Errors occurred while creating a socket by a provided port");
        }

    }

    @Override
    public void close() {
        helloServer.close();
    }

    private byte[] makeServerMessage(DatagramPacket received) {
        return ("Hello, " + new String(received.getData(), received.getOffset(),
                received.getLength(), CommonServer.CHARSET)).getBytes(CommonServer.CHARSET);
    }

    private void server() {
        try {
            var socket = helloServer.socket; // briefer
            var receiveBufferSize = socket.getReceiveBufferSize();
            while (!socket.isClosed()) {

                final DatagramPacket request = new DatagramPacket(new byte[receiveBufferSize], receiveBufferSize);

                try {
                    socket.receive(request);
                } catch (SocketTimeoutException e) {
                    System.err.println("Timeout of receiving expired. " + e.getMessage());
                    return;
                } catch (IOException e) {
                    if (!socket.isClosed()) {
                        System.err.println("Errors occurred while receiving a request message. "
                                + e.getMessage());
                        return;
                    }
                }

                var response = helloServer.getExecutors().submit(() -> {
                    int clientPort = request.getPort();
                    final InetAddress clientAddress = request.getAddress();
                    byte[] message = makeServerMessage(request);
                    return new DatagramPacket(message, message.length, clientAddress, clientPort);
                });

                try {
                    socket.send(response.get());
                } catch (IOException e) {
                    System.err.println("Errors occurred while sending a response message. "
                            + e.getMessage());
                } catch (InterruptedException e) {
                    System.err.println(e.getMessage());
                } catch (ExecutionException e) {
                    System.err.println("Error occurred while making a response message. "
                            + e.getMessage());
                }
            }

        } catch (SocketException e) {
            System.err.println("Errors occurred while accessing a socket by a provided port"
                    + e.getMessage());
        }

    }

    private static class InnerHello{
        private final ThreadPool threadPool;
        private final DatagramSocket socket;

        private InnerHello(int port, int threads) throws SocketException {
            threadPool = new ThreadPool(threads);
            socket = new DatagramSocket(port);
        }

        private void close() {
            socket.close();
            threadPool.close();
        }
        private ExecutorService getBeginner(){
            return threadPool.getBeginner();
        }
        private ExecutorService getExecutors(){
            return threadPool.getExecutors();
        }
    }
}
