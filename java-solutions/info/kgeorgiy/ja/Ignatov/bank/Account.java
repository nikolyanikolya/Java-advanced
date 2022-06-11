package info.kgeorgiy.ja.Ignatov.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface represents account at the bank with appropriate operations.
 * <p>
 * Objects implementing this interface are transmitted via remote links
 */
public interface Account extends Remote {
    /**
     * Returns account identifier.
     */
    String getId() throws RemoteException;

    /**
     * Returns amount of money at the account.
     */
    int getAmount() throws RemoteException;

    /**
     * Sets amount of money at the account.
     */
    void setAmount(int amount) throws RemoteException;
}
