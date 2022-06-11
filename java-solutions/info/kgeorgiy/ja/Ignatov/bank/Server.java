package info.kgeorgiy.ja.Ignatov.bank;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class Server {
    private final static int DEFAULT_PORT = 8888;

    public static void main(final String... args) {
        final Bank bank = new RemoteBank(DEFAULT_PORT);
        try {
            LocateRegistry.createRegistry(DEFAULT_PORT);
            UnicastRemoteObject.exportObject(bank, DEFAULT_PORT);
            Naming.rebind("//localhost:" + DEFAULT_PORT + "/bank", bank);
            System.out.println("Server started");
        } catch (final RemoteException e) {
            System.err.println("Cannot export object: " + e.getMessage());
        } catch (final MalformedURLException e) {
            System.err.println("Malformed URL. " + e.getMessage());
        }
    }
}
