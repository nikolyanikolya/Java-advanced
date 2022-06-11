package info.kgeorgiy.ja.Ignatov.bank;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Objects;

public class Client {
    private static final int PORT = 8888;
    private static final String OBJECT = "bank";
    private static final String URL = "//localhost:" + PORT + "/" + OBJECT;

    /**
     * Utility class.
     */
    private Client() {
    }

    private static String getId(String passport, String subId) {
        return passport + ":" + subId;
    }

    public static void main(final String... args) {

        final Bank bank;
        if (args == null || args.length != 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Illegal arguments. Usage: Client <name> " +
                    "<surname> <passport> <subId> <new_amount>");
            return;
        }
        try {
            try {
                bank = (Bank) Naming.lookup(URL);
            } catch (final NotBoundException e) {
                System.err.println("Bank is not bound. " + e.getMessage());
                return;
            } catch (final MalformedURLException e) {
                System.err.println("Bank URL is invalid. " + e.getMessage());
                return;
            }

            final String accountId = getId(args[2], args[3]);
            Person person = bank.getIndividual(args[2], false);
            if (person == null) {
                bank.createIndividual(args[0], args[1], args[2]);
                person = bank.getIndividual(args[2], false);
            } else {
                if (!person.getName().equals(args[0]) || !person.getSurName().equals(args[1])) {
                    System.err.println("Incorrect person name or surname. ");
                    return;
                }
            }
            Account account = bank.getAccount(person, accountId);
            if (account == null) {
                System.out.println("Creating account");
                account = bank.createAccount(accountId);
            } else {
                System.out.println("Account already exists");
            }
            System.out.println("Account id: " + account.getId());
            System.out.println("Money: " + account.getAmount());
            System.out.println("Adding money");
            account.setAmount(account.getAmount() + Integer.parseInt(args[4]));
            System.out.println("Money: " + account.getAmount());
        } catch (RemoteException e) {
            System.err.println(e.getMessage());
        }
    }
}
