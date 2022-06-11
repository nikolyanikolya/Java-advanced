package info.kgeorgiy.ja.Ignatov.hello;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class CommonServer {
    /*package-private*/ static final Charset CHARSET = StandardCharsets.UTF_8;
    /*package-private*/ static void runApplication(String[] args) {
        if (args == null || args.length != 2) {
            System.err.println("Invalid arguments. Usage: HelloUDPServer <port> <threads number>");
        } else {
            new HelloUDPNonblockingServer().start(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        }
    }
}
