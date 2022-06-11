package info.kgeorgiy.ja.Ignatov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Ignatov Nikolay
 */

public class HelloUDPClient implements HelloClient {
    private static final int QUERY_TIMEOUT = 20;
    private static final int SOCKET_TIMEOUT = 50;

    public static void main(String[] args) {
        CommonClient.runApplication(args);
    }
    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            System.err.println("Unknown host. " + e.getMessage());
            return;
        }

        try {
            final ExecutorService executorService = Executors.newFixedThreadPool(threads);
            for (int i = 0; i < threads; i++) {
                int finalI = i;
                executorService.submit(() -> client(requests, prefix, finalI, inetAddress, port));
            }
            executorService.shutdown();
            if (!executorService.awaitTermination(
                    (long) requests * threads * QUERY_TIMEOUT, TimeUnit.SECONDS)) {
                System.err.println("Pool did not terminate...");
            }
        } catch (InterruptedException e) {
            System.err.println("Some threads were interrupted. " + e.getMessage());
        }
    }

    private void client(int requests, String prefix, int ind, InetAddress address, int port) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(SOCKET_TIMEOUT);
            int receiveBufferSize = socket.getReceiveBufferSize();
            for (int j = 0; j < requests; j++) {
                String requestMessage = CommonClient.makeRequest(prefix, ind, j);
                while (!socket.isClosed()) {
                    byte[] message = requestMessage.getBytes(CommonClient.CHARSET);
                    final DatagramPacket request = new DatagramPacket
                            (
                                    message, 0, message.length,
                                    address, port
                            );

                    try {
                        socket.send(request);
                    } catch (IOException e) {
                        System.err.println("Errors occurred while sending the request message. " + e.getMessage());
                    }

                    final DatagramPacket response = new DatagramPacket(new byte[receiveBufferSize], receiveBufferSize);

                    try {
                        socket.receive(response);
                    } catch (SocketTimeoutException e) {
                        System.err.println("Timeout of receiving expired. " + e.getMessage());
                    } catch (IOException e) {
                        System.err.println("I/O Errors occurred while receiving the response message. " + e.getMessage());
                    }

                    final String responseMessage = new String(response.getData(), 0,
                                    response.getLength(), CommonClient.CHARSET);
                    boolean isSuccessful = CommonClient.isSuccessfulRequest(responseMessage, requestMessage);
                    if (isSuccessful) {
                        CommonClient.printMessage(responseMessage, requestMessage);
                        break;
                    }

                }
            }

        } catch (SocketException e) {
            System.err.println("Errors occurred while creating or accessing a socket in the thread "
                    + ind + ". " + e.getMessage());
        }
    }
}
