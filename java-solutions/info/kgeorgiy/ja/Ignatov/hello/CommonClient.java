package info.kgeorgiy.ja.Ignatov.hello;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class CommonClient {
    /*package-private*/ static final Charset CHARSET = StandardCharsets.UTF_8;

    /*package-private*/ static void runApplication(String[] args) {
        if (args == null || args.length != 5) {
            System.err.println("Invalid arguments. Usage: HelloUDPClient" +
                    "<ip> <port> <prefix> <stream number> <request number>");

        } else {
            new HelloUDPClient().run(args[0], Integer.parseInt(args[1]),
                    args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]));
        }
    }

    /*package-private*/ static boolean isSuccessfulRequest(String response, String request) {
        return response.equals("Hello, " + request);
    }
    /*package-private*/ static void printMessage(String response, String request) {
        System.out.println("Sent message: " + "'" + request + "'"
                + ". Received message: " + "'" + response + "'");
    }
    /*package-private*/ static String makeRequest(String prefix, int id, int requestNumber) {
        return prefix + id + "_" + requestNumber;
    }
}
